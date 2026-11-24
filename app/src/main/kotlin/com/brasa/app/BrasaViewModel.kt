package com.brasa.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brasa.app.data.AppSettings
import com.brasa.app.data.Capabilities
import com.brasa.app.data.ServerClient
import com.brasa.app.data.ActivityLevel
import com.brasa.app.data.Situation
import com.brasa.app.data.levelOf
import com.brasa.app.data.Day
import com.brasa.app.data.HealthEvent
import com.brasa.app.data.HistoryStore
import com.brasa.app.data.PreferencesStore
import com.brasa.app.data.UserRecord
import com.brasa.app.data.HealthRepository
import com.brasa.app.data.BreathSession
import com.brasa.app.domain.HeartRateSample
import com.brasa.app.domain.Rules
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Momento que merece celebrarse (vibración; la subida de nivel además tiene animación). */
sealed interface Celebration {
  data object Goal : Celebration
  data class Level(val level: Int, val stage: Stage, val newStage: Boolean) : Celebration
}

/** De dónde salen los datos. */
enum class DataSource { DEMO, SENSORS, MANUAL }

/** Estado listo para pintar. */
data class BrasaState(
  val emotion: Emotion,
  val stage: Stage,
  val level: Int,
  val xp: Int,
  val levelProgress: Float = 0f,
  val streak: Int = 0,
  val steps: Int,
  val goal: Int,
  val previousGoal: Int = goal,
  val progress: Float,
  val heartRate: Int?,
  val heartRateElevated: Boolean = false,
  /** Índice de la frase local (cambia al tocar a Brasa o al cambiar de emoción). */
  val phrase: Int,
  val greeting: Boolean,
  val source: DataSource = DataSource.DEMO,
  val stepsPerHour: List<Int> = List(24) { 0 },
  val heartRates: List<HeartRateSample> = emptyList(),
  val breathSessions: List<BreathSession> = emptyList(),
  val history: List<Int> = emptyList(),
  /** Frase de la IA para la situación actual (null = usar el banco local). */
  val aiPhrase: String? = null,
  /** Explicación de la meta (IA o null = frase local). */
  val goalPhrase: String? = null,
)

private fun todayEpoch() = LocalDate.now().toEpochDay()
private fun hourOf(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour

class BrasaViewModel(app: Application) : AndroidViewModel(app) {
  private val prefs = PreferencesStore(app)
  private val historyStore = HistoryStore(app)
  private val health = HealthRepository(app)
  private val server = ServerClient()

  val settings: StateFlow<AppSettings> = prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

  private val record = MutableStateFlow(UserRecord(Day(todayEpoch())))
  private val capabilities = MutableStateFlow(Capabilities(false, false))
  private val phrase = MutableStateFlow(0)
  private val greeting = MutableStateFlow(true)
  private val currentTime = MutableStateFlow(System.currentTimeMillis())
  private val loaded = MutableStateFlow(false)
  /** Caché de frases de la IA por "idioma:situación" (se piden solo al cambiar de situación). */
  private val aiPhrases = MutableStateFlow<Map<String, String>>(emptyMap())
  private val situation = MutableStateFlow<Situation?>(Situation.GREETING)
  private val goalPhrases = MutableStateFlow<Map<String, String>>(emptyMap())
  private var deviceId: String? = null
  private var lastRequestMs = 0L
  private var retryJob: kotlinx.coroutines.Job? = null
  private var lastBreathMs = 0L
  private val _celebrations = kotlinx.coroutines.flow.MutableSharedFlow<Celebration>(extraBufferCapacity = 4)
  /** Meta cumplida y subidas de nivel ocurridas mientras la app está abierta. */
  val celebrations: kotlinx.coroutines.flow.SharedFlow<Celebration> = _celebrations

  /** true cuando hay que mostrar la pantalla que explica los permisos (solo la primera vez). */
  val askPermissions: StateFlow<Boolean> =
    combine(record, loaded, settings) { r, c, a -> c && !a.demo && !r.permissionsAsked }
      .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val state: StateFlow<BrasaState> =
    combine(
      combine(combine(record, settings, capabilities, ::Triple), phrase, greeting, currentTime) { (r, a, c), f, s, t -> compute(r, a, c, f, s, t) },
      aiPhrases, situation, goalPhrases,
    ) { e, ia, currentSituation, goals ->
      // En demo la meta es de ejemplo: su explicación es la local (la de la IA describe la meta real).
      e.copy(aiPhrase = currentSituation?.let { ia["${language()}:${it.api}"] }, goalPhrase = if (e.source == DataSource.DEMO) null else goals[language()])
    }.stateIn(viewModelScope, SharingStarted.Eagerly, demoState(AppSettings(demo = true), 0, true))

  init {
    viewModelScope.launch {
      record.value = historyStore.load(todayEpoch())
      loaded.value = true
      startSensors()
      deviceId = prefs.deviceId()
      requestPhrase(Situation.GREETING)
      requestGoalExplanation()
    }
    viewModelScope.launch { health.events.collect(::applyHealthEvent) }
    // El saludo dura unos segundos; después Brasa habla según su emoción.
    viewModelScope.launch { delay(GREETING_MS); greeting.value = false; situation.value = situationFor(state.value.emotion); situation.value?.let(::requestPhrase) }
    // Celebraciones: solo cambios dentro de la misma fuente (activar o quitar el demo no es un logro)
    // y después de cargar el registro (el estado inicial no cuenta).
    viewModelScope.launch {
      var previousState: BrasaState? = null
      state.collect { e ->
        val p = previousState
        if (loaded.value) previousState = e
        if (p == null || !loaded.value || p.source != e.source) return@collect
        if (Rules.reachedGoal(p.progress, e.progress)) _celebrations.tryEmit(Celebration.Goal)
        if (Rules.leveledUp(p.level, e.level)) {
          _celebrations.tryEmit(Celebration.Level(e.level, e.stage, Rules.newStage(p.level, e.level) != null))
          situation.value = Situation.LEVEL_UP
          requestPhrase(Situation.LEVEL_UP, urgent = true)
        }
      }
    }
    // Al cambiar de emoción, frase nueva (y deja de saludar).
    viewModelScope.launch {
      var previousEmotion: Emotion? = null
      state.collect { e ->
        // Tras respirar, Brasa mantiene un minuto su frase de reconocimiento aunque cambie de emoción.
        val afterBreathing = System.currentTimeMillis() - lastBreathMs < 60_000
        if (previousEmotion != null && e.emotion != previousEmotion && !afterBreathing) {
          greeting.value = false; phrase.update { it + 1 }
          situation.value = situationFor(e.emotion)
          situation.value?.let(::requestPhrase)
        }
        previousEmotion = e.emotion
      }
    }
    // Reloj interno: la tensión y la inactividad dependen del tiempo, no solo de nuevos datos.
    viewModelScope.launch { while (true) { delay(30_000); currentTime.value = System.currentTimeMillis(); checkDay() } }
  }

  fun startSensors() = viewModelScope.launch { capabilities.value = health.start() }

  fun permissionsAnswered() {
    updateRecord { it.copy(permissionsAsked = true) }
    startSensors()
  }

  private fun checkDay() {
    val today = todayEpoch()
    if (record.value.today.epochDay != today) updateRecord { HistoryStore.rollOverDay(it, today) }
  }

  private fun updateRecord(f: (UserRecord) -> UserRecord) {
    record.update(f)
    val r = record.value
    viewModelScope.launch { historyStore.save(r) }
  }

  /** Aplica un dato de salud: pasos por hora, último movimiento y pulso (1 muestra/min como mucho). */
  private fun applyHealthEvent(e: HealthEvent) {
    checkDay()
    val t = System.currentTimeMillis()
    updateRecord { r ->
      val d = r.today
      val newValue = when (e) {
        is HealthEvent.Steps -> {
          val delta = (e.today - d.steps).coerceAtLeast(0)
          val perHour = d.stepsPerHour.toMutableList().also { it[hourOf(t)] += delta }
          d.copy(
            steps = maxOf(d.steps, e.today), stepsPerHour = perHour,
            lastMovementMs = if (delta >= 15) t else d.lastMovementMs,
          )
        }
        is HealthEvent.HeartRate -> d.copy(heartRates = addHeartRate(d.heartRates, HeartRateSample(e.ms, e.bpm)))
      }
      r.copy(today = newValue)
    }
    currentTime.value = t
  }

  private fun compute(r: UserRecord, a: AppSettings, c: Capabilities, f: Int, s: Boolean, t: Long): BrasaState {
    if (a.demo) return demoState(a, f, s)
    val d = r.today
    val source = if (c.steps || c.heartRate) DataSource.SENSORS else DataSource.MANUAL
    val progress = if (r.goal > 0) d.steps.toFloat() / r.goal else 0f
    val base = Rules.restingBaseline(d.heartRates.map { it.bpm })
    val elevated = c.heartRate && Rules.isHeartRateElevatedAtRest(d.heartRates, base, d.lastMovementMs, t)
    val idle = if (c.steps && d.lastMovementMs > 0) ((t - d.lastMovementMs) / 60_000).toInt() else 0
    val xp = r.previousXp + Rules.xpForSteps(d.steps) + r.totalBreaths * Rules.XP_PER_BREATH
    val heartRate = d.heartRates.lastOrNull()?.takeIf { t - it.ms < 10 * 60_000 }?.bpm
    return BrasaState(
      emotion = Rules.emotion(progress, elevated, idle),
      stage = Rules.stage(xp), level = Rules.level(xp), xp = xp, levelProgress = Rules.levelProgress(xp),
      streak = Rules.streak(r.history + d.steps, r.goal).coerceAtLeast(Rules.streak(r.history, r.goal)),
      steps = d.steps, goal = r.goal, previousGoal = r.previousGoal, progress = progress,
      heartRate = heartRate, heartRateElevated = elevated, phrase = f, greeting = s, source = source,
      stepsPerHour = d.stepsPerHour, heartRates = d.heartRates, breathSessions = d.breathSessions, history = r.history,
    )
  }

  /** Tocar a Brasa: deja de saludar y cambia de frase. En modo manual, además la alimenta (+250 pasos). */
  fun tapBrasa() {
    greeting.value = false
    phrase.update { it + 1 }
    // Al tocarla, Brasa varía con su banco local (la IA solo habla en momentos clave).
    situation.value = null
    if (!settings.value.demo && state.value.source == DataSource.MANUAL) {
      applyHealthEvent(HealthEvent.Steps(record.value.today.steps + STEPS_PER_TAP))
    }
  }

  fun theme(dark: Boolean) = viewModelScope.launch { prefs.dark(dark) }
  fun demo(v: Boolean) = viewModelScope.launch { prefs.demo(v) }
  fun demoEmotion(e: Emotion?) = viewModelScope.launch { prefs.demoEmotion(e); greeting.value = false }
  fun demoStage(e: Stage?) = viewModelScope.launch { prefs.demoStage(e) }

  /** Pulso al empezar a respirar: lectura reciente del sensor (o la del demo). */
  fun heartRateBeforeBreathing(): Int? = state.value.heartRate

  /** Pulso al terminar: la lectura más reciente si es nueva; en demo, una bajada típica. */
  fun heartRateAfterBreathing(before: Int): Int {
    if (settings.value.demo) return (before - 14).coerceAtLeast(55)
    val lastSample = record.value.today.heartRates.lastOrNull()
    return if (lastSample != null && System.currentTimeMillis() - lastSample.ms < 90_000) lastSample.bpm else before
  }

  /** Respiración completada: +10 XP y evento para la gráfica de pulso. Sale de la tensión. */
  fun breathCompleted(before: Int?, after: Int?) {
    val t = System.currentTimeMillis()
    lastBreathMs = t
    greeting.value = false
    updateRecord { r ->
      r.copy(
        totalBreaths = r.totalBreaths + 1,
        // Tras respirar, se considera "movimiento" para no volver a marcar tensión al instante.
        today = r.today.copy(breathSessions = r.today.breathSessions + BreathSession(t, before, after), lastMovementMs = t),
      )
    }
    if (settings.value.demo && settings.value.demoEmotion == Emotion.RESTLESS) demoEmotion(null)
    situation.value = Situation.POST_BREATHING
    requestPhrase(Situation.POST_BREATHING, urgent = true)
  }

  /** Idioma activo de la app (el de AppCompat o, si no hay, el del sistema). */
  private fun language(): String {
    val l = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0] ?: java.util.Locale.getDefault()
    return if (l.language == "es") "es" else "en"
  }

  private fun situationFor(e: Emotion): Situation? = when (e) {
    Emotion.RESTLESS -> Situation.TENSION
    Emotion.DIM -> Situation.INACTIVITY
    Emotion.ECSTATIC -> Situation.GOAL_REACHED
    else -> null // tranquila y feliz: banco local, sin gastar peticiones
  }

  /**
   * Pide una frase a la IA solo si no está en caché y han pasado ≥ 20 s desde la última petición
   * (cuota gratuita de Gemini). Solo viajan niveles agregados, nunca pasos ni pulso.
   */
  private fun requestPhrase(s: Situation, urgent: Boolean = false) {
    val id = deviceId ?: return
    val key = "${language()}:${s.api}"
    if (key in aiPhrases.value) return
    val t = System.currentTimeMillis()
    val waitMs = REQUEST_COOLDOWN_MS - (t - lastRequestMs)
    if (!urgent && waitMs > 0) {
      // Dentro de la ventana de 20 s: se reintenta al cumplirse si Brasa sigue en la misma situación.
      retryJob?.cancel()
      retryJob = viewModelScope.launch { delay(waitMs); if (situation.value == s) requestPhrase(s) }
      return
    }
    lastRequestMs = t
    val e = state.value
    viewModelScope.launch {
      server.message(id, language(), s, levelOf(e.progress), e.emotion.key.lowercase(), e.stage.key.lowercase())
        ?.let { f -> aiPhrases.update { it + (key to f) } }
    }
  }

  /** Explicación de la meta adaptativa (la meta ya la calculó el reloj con sus reglas). */
  private fun requestGoalExplanation() {
    val id = deviceId ?: return
    val r = record.value
    val trend = when { r.goal > r.previousGoal -> "sube"; r.goal < r.previousGoal -> "baja"; else -> "igual" }
    val previousDays = r.history.takeLast(6)
    val consistency = if (previousDays.isEmpty()) ActivityLevel.LOW else levelOf(previousDays.count { it >= r.goal } / previousDays.size.toFloat())
    val averageLevel = if (previousDays.isEmpty()) ActivityLevel.LOW else levelOf(previousDays.average().toFloat() / r.goal)
    val languageCode = language()
    viewModelScope.launch {
      server.goal(id, languageCode, trend, consistency, averageLevel)?.let { f -> goalPhrases.update { it + (languageCode to f) } }
    }
  }

  override fun onCleared() {
    viewModelScope.launch { health.stop() }
  }

  companion object {
    const val STEPS_PER_TAP = 250
    const val GREETING_MS = 6000L
    /** Cuota gratuita de Gemini: como mucho una frase cada 20 s por reloj. */
    const val REQUEST_COOLDOWN_MS = 20_000L
  }
}

/**
 * Añade una lectura de pulso: como mucho una por minuto (la nueva sustituye a la de ese minuto),
 * siempre ordenadas por hora aunque lleguen desordenadas (lotes de Health Services o el simulador).
 */
fun addHeartRate(heartRates: List<HeartRateSample>, m: HeartRateSample): List<HeartRateSample> =
  (heartRates.filterNot { kotlin.math.abs(it.ms - m.ms) < 60_000 } + m).sortedBy { it.ms }.takeLast(24 * 60)

/**
 * Modo demo: datos completos de ejemplo, sin servidor ni sensores; permite forzar emoción y etapa.
 * El día de ejemplo solo llega hasta [minutoActual] (nunca pinta horas futuras). Todo lo que se ve
 * en demo está marcado como tal en Ajustes ("Simulados (demo)").
 */
fun demoState(a: AppSettings, f: Int, s: Boolean, currentMinute: Int = minuteOfDay()): BrasaState {
  val goal = 8500
  val emotion = a.demoEmotion ?: Emotion.HAPPY
  val progress = when (emotion) {
    Emotion.DIM -> 0.12f; Emotion.RESTLESS -> 0.45f; Emotion.CALM -> 0.42f
    Emotion.HAPPY -> 0.73f; Emotion.ECSTATIC -> 1f
  }
  val stage = a.demoStage ?: Stage.FLAME
  val level = if (a.demoStage != null) stage.minLevel + 2 else 7
  val xp = Rules.xpForLevel(level) + 40
  val steps = (progress * goal).toInt()
  val heartRates = demoHeartRates(currentMinute)
  return BrasaState(
    emotion = emotion, stage = stage, level = level, xp = xp, levelProgress = Rules.levelProgress(xp), streak = 4,
    steps = steps, goal = goal, previousGoal = 8000, progress = progress,
    heartRate = if (emotion == Emotion.RESTLESS) 96 else heartRates.lastOrNull()?.bpm ?: 64, heartRateElevated = emotion == Emotion.RESTLESS,
    phrase = f, greeting = s, source = DataSource.DEMO,
    stepsPerHour = demoStepsPerHour(steps, currentMinute / 60), heartRates = heartRates, breathSessions = demoBreaths(currentMinute),
    history = listOf(7200, 9100, 5400, 8800, 9600, 4300),
  )
}

fun minuteOfDay(): Int = java.time.LocalTime.now().let { it.hour * 60 + it.minute }

/** Día típico: pico al ir al trabajo, al mediodía y por la tarde. */
fun demoStepsPerHour(total: Int, currentHour: Int = java.time.LocalTime.now().hour): List<Int> {
  val shape = listOf(0, 0, 0, 0, 0, 0, 2, 9, 12, 5, 4, 6, 10, 7, 4, 4, 6, 9, 13, 5, 3, 1, 0, 0)
    .mapIndexed { h, v -> if (h <= currentHour) v else 0 }
    // De madrugada: lo andado va a la hora actual (la suma por horas siempre coincide con el total).
    .let { f -> if (f.sum() == 0) f.mapIndexed { h, _ -> if (h == currentHour) 1 else 0 } else f }
  val weightSum = shape.sum().toFloat().coerceAtLeast(1f)
  return shape.map { (it / weightSum * total).toInt() }
}

private fun demoDayStart() = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Minuto del pico de tensión de ejemplo (16:10) y de la respiración que lo calma (16:25). */
private const val TENSION_MINUTE = 16 * 60 + 10
private const val BREATH_MINUTE = 16 * 60 + 25

/**
 * Pulso de ejemplo, una muestra cada 5 min desde las 00:00 hasta [minutoActual]: sueño ~54 lpm,
 * reposo de día ~64, caminatas a las 8 y a las 18 (~95), pico de tensión en reposo a las 16:10
 * que baja tras respirar. Variación pseudoaleatoria fija (siempre la misma curva).
 */
fun demoHeartRates(currentMinute: Int = minuteOfDay()): List<HeartRateSample> {
  val start = demoDayStart()
  val noise = kotlin.random.Random(7)
  return (0..currentMinute.coerceIn(0, 24 * 60 - 1) step 5).map { min ->
    val h = min / 60f
    val asleep = h < 6.5f || h > 23f
    val base = if (asleep) 54f else 64f + 4f * kotlin.math.sin((h - 9f) * 0.5f)
    val walking = if (h in 7.6f..8.4f || h in 18f..18.9f) 31f else 0f
    val tension = if (min in TENSION_MINUTE - 20..BREATH_MINUTE) 30f * (1f - kotlin.math.abs(min - TENSION_MINUTE) / 25f).coerceAtLeast(0f) else 0f
    HeartRateSample(start + min * 60_000L, (base + walking + tension + noise.nextInt(-3, 4)).toInt())
  }
}

fun demoBreaths(currentMinute: Int = minuteOfDay()): List<BreathSession> =
  if (currentMinute < BREATH_MINUTE) emptyList() else listOf(BreathSession(demoDayStart() + BREATH_MINUTE * 60_000L, 94, 78))
