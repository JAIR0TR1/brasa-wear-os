package com.brasa.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.brasa.app.data.Ajustes
import com.brasa.app.data.Preferencias
import com.brasa.app.domain.Reglas
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Datos del día que llegan de los sensores (o de la simulación del modo demo). */
data class DatosDia(
  val pasos: Int,
  val meta: Int,
  val pulsoReposo: Int?,
  val pulsoElevado: Boolean,
  val minutosQuieto: Int,
  val xp: Int,
)

/** Estado listo para pintar. */
data class EstadoBrasa(
  val emocion: Emocion,
  val etapa: Etapa,
  val nivel: Int,
  val xp: Int,
  val pasos: Int,
  val meta: Int,
  val progreso: Float,
  val pulso: Int?,
  /** Índice de la frase local (cambia al tocar a Brasa o al cambiar de emoción). */
  val frase: Int,
  val saludando: Boolean,
)

/** Datos de ejemplo del modo demo (fase 2; la fase 3 conecta Health Services). */
val DatosDemo = DatosDia(pasos = 6240, meta = 8500, pulsoReposo = 64, pulsoElevado = false, minutosQuieto = 25, xp = 780)

class BrasaViewModel(app: Application) : AndroidViewModel(app) {
  private val prefs = Preferencias(app)

  val ajustes: StateFlow<Ajustes> = prefs.ajustes.stateIn(viewModelScope, SharingStarted.Eagerly, Ajustes())

  private val datos = MutableStateFlow(DatosDemo)
  private val frase = MutableStateFlow(0)
  private val saludando = MutableStateFlow(true)

  val estado: StateFlow<EstadoBrasa> =
    combine(datos, ajustes, frase, saludando) { d, a, f, s -> calcular(d, a, f, s) }
      .stateIn(viewModelScope, SharingStarted.Eagerly, calcular(DatosDemo, Ajustes(), 0, true))

  private fun calcular(d: DatosDia, a: Ajustes, f: Int, s: Boolean): EstadoBrasa {
    val progreso = if (d.meta > 0) d.pasos.toFloat() / d.meta else 0f
    val auto = Reglas.emocion(progreso, d.pulsoElevado, d.minutosQuieto)
    val emocion = if (a.demo) a.emocionDemo ?: auto else auto
    val etapa = if (a.demo) a.etapaDemo ?: Reglas.etapa(d.xp) else Reglas.etapa(d.xp)
    val nivel = if (a.demo && a.etapaDemo != null) a.etapaDemo.nivelMinimo else Reglas.nivel(d.xp)
    // En demo, forzar una emoción ajusta los datos mostrados para que sean coherentes.
    val progresoMostrado = if (a.demo && a.emocionDemo != null) progresoDe(a.emocionDemo) else progreso
    return EstadoBrasa(
      emocion = emocion, etapa = etapa, nivel = nivel, xp = d.xp,
      pasos = (progresoMostrado * d.meta).toInt(), meta = d.meta, progreso = progresoMostrado,
      pulso = if (emocion == Emocion.INQUIETA) 96 else d.pulsoReposo,
      frase = f, saludando = s,
    )
  }

  private fun progresoDe(e: Emocion) = when (e) {
    Emocion.APAGADA -> 0.12f
    Emocion.INQUIETA -> 0.45f
    Emocion.TRANQUILA -> 0.42f
    Emocion.FELIZ -> 0.73f
    Emocion.EUFORICA -> 1f
  }

  /** Tocar a Brasa: deja de saludar y cambia de frase. */
  fun tocarBrasa() {
    saludando.value = false
    frase.update { it + 1 }
  }

  fun tema(oscuro: Boolean) = viewModelScope.launch { prefs.oscuro(oscuro) }
  fun demo(v: Boolean) = viewModelScope.launch { prefs.demo(v) }
  fun emocionDemo(e: Emocion?) = viewModelScope.launch { prefs.emocionDemo(e); saludando.value = false }
  fun etapaDemo(e: Etapa?) = viewModelScope.launch { prefs.etapaDemo(e) }

  /** Pulso antes/después de respirar (demo; la fase 3 usa el sensor). */
  fun pulsoAntesRespirar(): Int = estado.value.pulso ?: 72
  fun pulsoDespuesRespirar(antes: Int): Int = (antes - 14).coerceAtLeast(55)
}
