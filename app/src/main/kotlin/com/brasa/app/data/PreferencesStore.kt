package com.brasa.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Dos almacenes:
 * - "brasa" (solo en este reloj, excluido de la copia de seguridad): registro del día con pasos y pulso
 *   exactos, historial de pasos por día e id anónimo del reloj.
 * - "brasa_respaldo" (copia de seguridad de Google del usuario, ver res/xml/reglas_extraccion.xml):
 *   ajustes y progreso sin datos de salud (XP total, respiraciones, meta).
 */
private val Context.mainStore by preferencesDataStore(name = "brasa")
private val Context.backupStore by preferencesDataStore(name = BACKUP_STORE_NAME)
const val BACKUP_STORE_NAME = "brasa_respaldo"

/** Progreso que viaja a un reloj nuevo (nada de salud: ni pasos, ni pulso, ni historial por día). */
data class Progress(val totalXp: Int, val totalBreaths: Int, val goal: Int, val previousGoal: Int)

/** Ajustes del usuario. El idioma lo guarda AppCompat (autoStoreLocales), no aquí. */
data class AppSettings(
  val dark: Boolean = true,
  val demo: Boolean = false,
  /** null = automática (según los datos). */
  val demoEmotion: Emotion? = null,
  val demoStage: Stage? = null,
)

class PreferencesStore(private val ctx: Context) {
  private object K {
    val dark = booleanPreferencesKey("oscuro")
    val demo = booleanPreferencesKey("demo")
    val demoEmotion = stringPreferencesKey("emocion_demo")
    val demoStage = stringPreferencesKey("etapa_demo")
  }

  /** Ajustes del almacén con copia de seguridad (si faltan, los de versiones anteriores en "brasa"). */
  val settings: Flow<AppSettings> = combine(ctx.backupStore.data, ctx.mainStore.data) { r, legacy ->
    fun <T> readKey(k: Preferences.Key<T>): T? = r[k] ?: legacy[k]
    AppSettings(
      dark = readKey(K.dark) ?: true,
      demo = readKey(K.demo) ?: false,
      demoEmotion = readKey(K.demoEmotion)?.let { n -> Emotion.entries.firstOrNull { it.key == n } },
      demoStage = readKey(K.demoStage)?.let { n -> Stage.entries.firstOrNull { it.key == n } },
    )
  }

  /** Id aleatorio por reloj (UUID). No identifica a la persona: no hay registro ni cuenta. */
  suspend fun deviceId(): String {
    val key = stringPreferencesKey("dispositivo")
    ctx.mainStore.data.map { it[key] }.firstOrNull()?.let { return it }
    val newValue = java.util.UUID.randomUUID().toString()
    ctx.mainStore.edit { it[key] = newValue }
    return newValue
  }

  suspend fun dark(v: Boolean) = ctx.backupStore.edit { it[K.dark] = v }
  suspend fun demo(v: Boolean) = ctx.backupStore.edit { it[K.demo] = v }
  suspend fun demoEmotion(e: Emotion?) = ctx.backupStore.edit { if (e == null) it.remove(K.demoEmotion) else it[K.demoEmotion] = e.key }
  suspend fun demoStage(e: Stage?) = ctx.backupStore.edit { if (e == null) it.remove(K.demoStage) else it[K.demoStage] = e.key }
}


/** Una respiración guiada completada (para la gráfica de pulso). */
@kotlinx.serialization.Serializable
data class BreathSession(
  val ms: Long,
  @SerialName("antes") val before: Int?,
  @SerialName("despues") val after: Int?,
)

/** Datos de un día. Todo se queda en el reloj (privacidad: nunca se envía en bruto). */
@kotlinx.serialization.Serializable
data class Day(
  val epochDay: Long,
  @SerialName("pasos") val steps: Int = 0,
  @SerialName("pasosPorHora") val stepsPerHour: List<Int> = List(24) { 0 },
  /** Pulso del día, como mucho una muestra por minuto. */
  @SerialName("pulsos") val heartRates: List<com.brasa.app.domain.HeartRateSample> = emptyList(),
  @SerialName("respiros") val breathSessions: List<BreathSession> = emptyList(),
  @SerialName("ultimoMovimientoMs") val lastMovementMs: Long = 0,
)

@kotlinx.serialization.Serializable
data class UserRecord(
  @SerialName("hoy") val today: Day,
  /** Pasos de los últimos 7 días completos (el último = ayer). */
  @SerialName("historial") val history: List<Int> = emptyList(),
  @SerialName("meta") val goal: Int = com.brasa.app.domain.Rules.GOAL_INITIAL,
  /** Meta del día anterior, para que la IA explique el cambio. */
  @SerialName("metaAnterior") val previousGoal: Int = com.brasa.app.domain.Rules.GOAL_INITIAL,
  /** XP acumulada de días anteriores. */
  @SerialName("xpPrevio") val previousXp: Int = 0,
  @SerialName("respirosTotales") val totalBreaths: Int = 0,
  @SerialName("permisosPreguntados") val permissionsAsked: Boolean = false,
)

/** Persistencia del registro y cambio de día (meta adaptativa + XP). */
class HistoryStore(private val ctx: Context) {
  private val key = stringPreferencesKey("registro")
  private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
  private object R {
    val xp = intPreferencesKey("xp_total")
    val breathSessions = intPreferencesKey("respiros_totales")
    val goal = intPreferencesKey("meta")
    val previousGoal = intPreferencesKey("meta_anterior")
  }

  /** Registro de este reloj; si no hay (reloj nuevo o datos borrados), se recupera el progreso de la copia. */
  suspend fun load(today: Long): UserRecord {
    val text = ctx.mainStore.data.map { it[key] }.firstOrNull()
    val r = text?.let { runCatching { json.decodeFromString(UserRecord.serializer(), it) }.getOrNull() }
      ?: fromBackup(savedProgress(), today)
    return rollOverDay(r, today)
  }

  suspend fun save(r: UserRecord) {
    val text = json.encodeToString(UserRecord.serializer(), r)
    ctx.mainStore.edit { it[key] = text }
    val p = progressOf(r)
    ctx.backupStore.edit {
      it[R.xp] = p.totalXp; it[R.breathSessions] = p.totalBreaths; it[R.goal] = p.goal; it[R.previousGoal] = p.previousGoal
    }
  }

  private suspend fun savedProgress(): Progress? = ctx.backupStore.data.firstOrNull()?.let { p ->
    val xp = p[R.xp] ?: return@let null
    Progress(xp, p[R.breathSessions] ?: 0, p[R.goal] ?: com.brasa.app.domain.Rules.GOAL_INITIAL, p[R.previousGoal] ?: com.brasa.app.domain.Rules.GOAL_INITIAL)
  }

  companion object {
    /** Lo que se guarda en la copia: XP total (días anteriores + hoy + respiraciones), respiraciones y meta. */
    fun progressOf(r: UserRecord) = Progress(
      totalXp = r.previousXp + com.brasa.app.domain.Rules.xpForSteps(r.today.steps) + r.totalBreaths * com.brasa.app.domain.Rules.XP_PER_BREATH,
      totalBreaths = r.totalBreaths, goal = r.goal, previousGoal = r.previousGoal,
    )

    /**
     * Reloj nuevo: mismo nivel, XP y meta; el día empieza de cero (los pasos de hoy ya están dentro de la XP)
     * y los permisos se vuelven a pedir porque son de cada reloj.
     */
    fun fromBackup(p: Progress?, today: Long): UserRecord {
      if (p == null) return UserRecord(Day(today))
      return UserRecord(
        today = Day(today),
        previousXp = (p.totalXp - p.totalBreaths * com.brasa.app.domain.Rules.XP_PER_BREATH).coerceAtLeast(0),
        totalBreaths = p.totalBreaths, goal = p.goal, previousGoal = p.previousGoal,
      )
    }

    /** Si cambió el día: cierra ayer en el historial, recalcula la meta y suma la XP. */
    fun rollOverDay(r: UserRecord, today: Long): UserRecord {
      if (r.today.epochDay == today) return r
      val history = (r.history + r.today.steps).takeLast(7)
      return r.copy(
        today = Day(today),
        history = history,
        previousGoal = r.goal,
        goal = com.brasa.app.domain.Rules.adaptiveGoal(history, r.goal),
        previousXp = r.previousXp + com.brasa.app.domain.Rules.xpForSteps(r.today.steps),
      )
    }
  }
}
