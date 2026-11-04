package com.brasa.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brasa.app.data.Ajustes
import com.brasa.app.data.Capacidades
import com.brasa.app.data.Dia
import com.brasa.app.data.EventoSalud
import com.brasa.app.data.Historial
import com.brasa.app.data.Preferencias
import com.brasa.app.data.Registro
import com.brasa.app.data.RepositorioSalud
import com.brasa.app.data.Respiro
import com.brasa.app.domain.Muestra
import com.brasa.app.domain.Reglas
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
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

/** De dónde salen los datos. */
enum class Fuente { DEMO, SENSORES, MANUAL }

/** Estado listo para pintar. */
data class EstadoBrasa(
  val emocion: Emocion,
  val etapa: Etapa,
  val nivel: Int,
  val xp: Int,
  val progresoNivel: Float = 0f,
  val racha: Int = 0,
  val pasos: Int,
  val meta: Int,
  val metaAnterior: Int = meta,
  val progreso: Float,
  val pulso: Int?,
  val pulsoElevado: Boolean = false,
  /** Índice de la frase local (cambia al tocar a Brasa o al cambiar de emoción). */
  val frase: Int,
  val saludando: Boolean,
  val fuente: Fuente = Fuente.DEMO,
  val pasosPorHora: List<Int> = List(24) { 0 },
  val pulsos: List<Muestra> = emptyList(),
  val respiros: List<Respiro> = emptyList(),
  val historial: List<Int> = emptyList(),
)

private fun hoyEpoch() = LocalDate.now().toEpochDay()
private fun horaDe(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour

class BrasaViewModel(app: Application) : AndroidViewModel(app) {
  private val prefs = Preferencias(app)
  private val historial = Historial(app)
  private val salud = RepositorioSalud(app)

  val ajustes: StateFlow<Ajustes> = prefs.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())

  private val registro = MutableStateFlow(Registro(Dia(hoyEpoch())))
  private val capacidades = MutableStateFlow(Capacidades(false, false))
  private val frase = MutableStateFlow(0)
  private val saludando = MutableStateFlow(true)
  private val ahora = MutableStateFlow(System.currentTimeMillis())
  private val cargado = MutableStateFlow(false)

  /** true cuando hay que mostrar la pantalla que explica los permisos (solo la primera vez). */
  val pedirPermisos: StateFlow<Boolean> =
    combine(registro, cargado, ajustes) { r, c, a -> c && !a.demo && !r.permisosPreguntados }
      .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val estado: StateFlow<EstadoBrasa> =
    combine(combine(registro, ajustes, capacidades, ::Triple), frase, saludando, ahora) { (r, a, c), f, s, t ->
      calcular(r, a, c, f, s, t)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, estadoDemo(Ajustes(demo = true), 0, true))

  init {
    viewModelScope.launch {
      registro.value = historial.cargar(hoyEpoch())
      cargado.value = true
      iniciarSensores()
    }
    viewModelScope.launch { salud.eventos.collect(::aplicar) }
    // El saludo dura unos segundos; después Brasa habla según su emoción.
    viewModelScope.launch { delay(SALUDO_MS); saludando.value = false }
    // Al cambiar de emoción, frase nueva (y deja de saludar).
    viewModelScope.launch {
      var anterior: Emocion? = null
      estado.collect { e ->
        if (anterior != null && e.emocion != anterior) { saludando.value = false; frase.update { it + 1 } }
        anterior = e.emocion
      }
    }
    // Reloj interno: la tensión y la inactividad dependen del tiempo, no solo de nuevos datos.
    viewModelScope.launch { while (true) { delay(30_000); ahora.value = System.currentTimeMillis(); revisarDia() } }
  }

  fun iniciarSensores() = viewModelScope.launch { capacidades.value = salud.iniciar() }

  fun permisosRespondidos() {
    actualizar { it.copy(permisosPreguntados = true) }
    iniciarSensores()
  }

  private fun revisarDia() {
    val hoy = hoyEpoch()
    if (registro.value.hoy.epochDay != hoy) actualizar { Historial.cambiarDia(it, hoy) }
  }

  private fun actualizar(f: (Registro) -> Registro) {
    registro.update(f)
    val r = registro.value
    viewModelScope.launch { historial.guardar(r) }
  }

  /** Aplica un dato de salud: pasos por hora, último movimiento y pulso (1 muestra/min como mucho). */
  private fun aplicar(e: EventoSalud) {
    revisarDia()
    val t = System.currentTimeMillis()
    actualizar { r ->
      val d = r.hoy
      val nuevo = when (e) {
        is EventoSalud.Pasos -> {
          val delta = (e.hoy - d.pasos).coerceAtLeast(0)
          val porHora = d.pasosPorHora.toMutableList().also { it[horaDe(t)] += delta }
          d.copy(
            pasos = maxOf(d.pasos, e.hoy), pasosPorHora = porHora,
            ultimoMovimientoMs = if (delta >= 15) t else d.ultimoMovimientoMs,
          )
        }
        is EventoSalud.Pulso -> {
          val ultima = d.pulsos.lastOrNull()
          val pulsos = if (ultima != null && e.ms - ultima.ms < 60_000) d.pulsos.dropLast(1) + Muestra(e.ms, e.bpm)
          else d.pulsos + Muestra(e.ms, e.bpm)
          d.copy(pulsos = pulsos.takeLast(24 * 60))
        }
      }
      r.copy(hoy = nuevo)
    }
    ahora.value = t
  }

  private fun calcular(r: Registro, a: Ajustes, c: Capacidades, f: Int, s: Boolean, t: Long): EstadoBrasa {
    if (a.demo) return estadoDemo(a, f, s)
    val d = r.hoy
    val fuente = if (c.pasos || c.pulso) Fuente.SENSORES else Fuente.MANUAL
    val progreso = if (r.meta > 0) d.pasos.toFloat() / r.meta else 0f
    val base = Reglas.baseReposo(d.pulsos.map { it.bpm })
    val elevado = c.pulso && Reglas.pulsoElevadoEnReposo(d.pulsos, base, d.ultimoMovimientoMs, t)
    val quieto = if (c.pasos && d.ultimoMovimientoMs > 0) ((t - d.ultimoMovimientoMs) / 60_000).toInt() else 0
    val xp = r.xpPrevio + Reglas.xpPorPasos(d.pasos) + r.respirosTotales * Reglas.XP_POR_RESPIRACION
    val pulso = d.pulsos.lastOrNull()?.takeIf { t - it.ms < 10 * 60_000 }?.bpm
    return EstadoBrasa(
      emocion = Reglas.emocion(progreso, elevado, quieto),
      etapa = Reglas.etapa(xp), nivel = Reglas.nivel(xp), xp = xp, progresoNivel = Reglas.progresoNivel(xp),
      racha = Reglas.racha(r.historial + d.pasos, r.meta).coerceAtLeast(Reglas.racha(r.historial, r.meta)),
      pasos = d.pasos, meta = r.meta, metaAnterior = r.metaAnterior, progreso = progreso,
      pulso = pulso, pulsoElevado = elevado, frase = f, saludando = s, fuente = fuente,
      pasosPorHora = d.pasosPorHora, pulsos = d.pulsos, respiros = d.respiros, historial = r.historial,
    )
  }

  /** Tocar a Brasa: deja de saludar y cambia de frase. En modo manual, además la alimenta (+250 pasos). */
  fun tocarBrasa() {
    saludando.value = false
    frase.update { it + 1 }
    if (!ajustes.value.demo && estado.value.fuente == Fuente.MANUAL) {
      aplicar(EventoSalud.Pasos(registro.value.hoy.pasos + PASOS_POR_TOQUE))
    }
  }

  fun tema(oscuro: Boolean) = viewModelScope.launch { prefs.oscuro(oscuro) }
  fun demo(v: Boolean) = viewModelScope.launch { prefs.demo(v) }
  fun emocionDemo(e: Emocion?) = viewModelScope.launch { prefs.emocionDemo(e); saludando.value = false }
  fun etapaDemo(e: Etapa?) = viewModelScope.launch { prefs.etapaDemo(e) }

  /** Pulso al empezar a respirar: lectura reciente del sensor (o la del demo). */
  fun pulsoAntesRespirar(): Int? = estado.value.pulso

  /** Pulso al terminar: la lectura más reciente si es nueva; en demo, una bajada típica. */
  fun pulsoDespuesRespirar(antes: Int): Int {
    if (ajustes.value.demo) return (antes - 14).coerceAtLeast(55)
    val ultima = registro.value.hoy.pulsos.lastOrNull()
    return if (ultima != null && System.currentTimeMillis() - ultima.ms < 90_000) ultima.bpm else antes
  }

  /** Respiración completada: +10 XP y evento para la gráfica de pulso. Sale de la tensión. */
  fun respiroCompletado(antes: Int?, despues: Int?) {
    val t = System.currentTimeMillis()
    actualizar { r ->
      r.copy(
        respirosTotales = r.respirosTotales + 1,
        // Tras respirar, se considera "movimiento" para no volver a marcar tensión al instante.
        hoy = r.hoy.copy(respiros = r.hoy.respiros + Respiro(t, antes, despues), ultimoMovimientoMs = t),
      )
    }
    if (ajustes.value.demo && ajustes.value.emocionDemo == Emocion.INQUIETA) emocionDemo(null)
  }

  override fun onCleared() {
    viewModelScope.launch { salud.detener() }
  }

  companion object {
    const val PASOS_POR_TOQUE = 250
    const val SALUDO_MS = 6000L
  }
}

/** Modo demo: datos completos de ejemplo, sin servidor ni sensores; permite forzar emoción y etapa. */
fun estadoDemo(a: Ajustes, f: Int, s: Boolean): EstadoBrasa {
  val meta = 8500
  val emocion = a.emocionDemo ?: Emocion.FELIZ
  val progreso = when (emocion) {
    Emocion.APAGADA -> 0.12f; Emocion.INQUIETA -> 0.45f; Emocion.TRANQUILA -> 0.42f
    Emocion.FELIZ -> 0.73f; Emocion.EUFORICA -> 1f
  }
  val etapa = a.etapaDemo ?: Etapa.LLAMA
  val nivel = if (a.etapaDemo != null) etapa.nivelMinimo + 2 else 7
  val xp = Reglas.xpParaNivel(nivel) + 40
  val pasos = (progreso * meta).toInt()
  return EstadoBrasa(
    emocion = emocion, etapa = etapa, nivel = nivel, xp = xp, progresoNivel = Reglas.progresoNivel(xp), racha = 4,
    pasos = pasos, meta = meta, metaAnterior = 8000, progreso = progreso,
    pulso = if (emocion == Emocion.INQUIETA) 96 else 64, pulsoElevado = emocion == Emocion.INQUIETA,
    frase = f, saludando = s, fuente = Fuente.DEMO,
    pasosPorHora = demoPasosPorHora(pasos), pulsos = demoPulsos(), respiros = demoRespiros(),
    historial = listOf(7200, 9100, 5400, 8800, 9600, 4300),
  )
}

/** Día típico: pico al ir al trabajo, al mediodía y por la tarde. */
fun demoPasosPorHora(total: Int): List<Int> {
  val forma = listOf(0, 0, 0, 0, 0, 0, 2, 9, 12, 5, 4, 6, 10, 7, 4, 4, 6, 9, 13, 5, 3, 1, 0, 0)
  val suma = forma.sum().toFloat()
  return forma.map { (it / suma * total).toInt() }
}

private val inicioDemo = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Pulso de ejemplo: reposo ~62, subida al caminar y pico de tensión a las 16:20 que baja tras respirar. */
fun demoPulsos(): List<Muestra> = (6 * 60 until 22 * 60 step 10).map { min ->
  val h = min / 60f
  val base = 62 + 8 * kotlin.math.sin(h * 0.9f) + if (h in 7.5f..8.5f || h in 18f..19f) 30 else 0
  val tension = if (h in 15.9f..16.4f) 34 * (1 - kotlin.math.abs(h - 16.2f) / 0.35f).coerceAtLeast(0f) else 0f
  Muestra(inicioDemo + min * 60_000L, (base + tension).toInt())
}

fun demoRespiros(): List<Respiro> = listOf(Respiro(inicioDemo + (16 * 60 + 25) * 60_000L, 96, 82))
