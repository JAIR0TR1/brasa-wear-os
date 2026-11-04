package com.brasa.app.domain

import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Reglas deterministas de Brasa (puras, sin Android: se prueban con JUnit).
 * Documentadas en docs/direccion-de-arte.md y README.
 */
object Reglas {
  /** Horas sin moverse a partir de las cuales Brasa se apaga aunque la meta vaya bien. */
  const val MINUTOS_QUIETO_APAGADA = 180

  /**
   * Emoción a partir del estado del día.
   * Prioridad: tensión (pulso elevado en reposo) > meta cumplida > inactividad/meta baja > progreso.
   */
  fun emocion(progreso: Float, pulsoElevadoEnReposo: Boolean, minutosQuieto: Int): Emocion = when {
    pulsoElevadoEnReposo -> Emocion.INQUIETA
    progreso >= 1f -> Emocion.EUFORICA
    progreso < 0.2f || minutosQuieto >= MINUTOS_QUIETO_APAGADA -> Emocion.APAGADA
    progreso < 0.6f -> Emocion.TRANQUILA
    else -> Emocion.FELIZ
  }

  /** 1 XP por cada 100 pasos; 10 XP por respiración completada. */
  fun xpPorPasos(pasos: Int): Int = pasos / 100
  const val XP_POR_RESPIRACION = 10

  /** Nivel = 1 + ⌊√(xp/25)⌋ → nivel 5 ≈ 400 XP (≈ 1 semana activa), 10 ≈ 2025 XP (≈ 1 mes), 20 ≈ 9025 XP. */
  fun nivel(xp: Int): Int = 1 + floor(sqrt(xp.coerceAtLeast(0) / 25.0)).toInt()

  /** XP necesaria para alcanzar un nivel. */
  fun xpParaNivel(nivel: Int): Int = 25 * (nivel - 1) * (nivel - 1)

  /** Progreso 0..1 dentro del nivel actual. */
  fun progresoNivel(xp: Int): Float {
    val n = nivel(xp)
    val a = xpParaNivel(n)
    val b = xpParaNivel(n + 1)
    return ((xp - a).toFloat() / (b - a)).coerceIn(0f, 1f)
  }

  fun etapa(xp: Int): Etapa = Etapa.deNivel(nivel(xp))

  // ---------- Tensión (señales, no diagnóstico) ----------

  /** Lpm por encima de la base en reposo que cuentan como "pulso elevado". */
  const val MARGEN_TENSION_LPM = 20
  /** Ventana de pulso que se promedia. */
  const val VENTANA_TENSION_MS = 3 * 60_000L
  /** Sin pasos durante este tiempo = "en reposo". */
  const val REPOSO_MS = 5 * 60_000L
  const val BASE_POR_DEFECTO = 65

  /** Base en reposo: percentil 20 de las muestras del día (robusto a picos). Con pocas muestras, 65 lpm. */
  fun baseReposo(bpm: List<Int>): Int {
    if (bpm.size < 10) return BASE_POR_DEFECTO
    val orden = bpm.sorted()
    return orden[(orden.size * 0.2).toInt()]
  }

  /**
   * Pulso elevado en reposo: la media de los últimos 3 min supera base + 20 lpm (con al menos 2 lecturas)
   * y no ha habido movimiento en 5 min. Si te estás moviendo, un pulso alto es normal.
   */
  fun pulsoElevadoEnReposo(muestras: List<Muestra>, base: Int, ultimoMovimientoMs: Long, ahoraMs: Long): Boolean {
    if (ahoraMs - ultimoMovimientoMs < REPOSO_MS) return false
    val recientes = muestras.filter { ahoraMs - it.ms <= VENTANA_TENSION_MS }
    if (recientes.size < 2) return false
    return recientes.map { it.bpm }.average() >= base + MARGEN_TENSION_LPM
  }

  // ---------- Meta adaptativa ----------

  const val META_MIN = 3000
  const val META_MAX = 15000
  const val META_INICIAL = 6000
  /** Cambio máximo de la meta de un día a otro (±15 %). */
  const val CAMBIO_MAX = 0.15

  /**
   * Meta de mañana = media de los últimos 7 días × 1,05 (un pequeño reto), redondeada a 100,
   * limitada a ±15 % de la meta actual y al rango 3.000–15.000. Sin historial: se mantiene.
   */
  fun metaAdaptativa(historial: List<Int>, metaActual: Int): Int {
    val dias = historial.takeLast(7).filter { it > 0 }
    if (dias.isEmpty()) return metaActual.coerceIn(META_MIN, META_MAX)
    val propuesta = dias.average() * 1.05
    val limitada = propuesta.coerceIn(metaActual * (1 - CAMBIO_MAX), metaActual * (1 + CAMBIO_MAX))
    return ((Math.round(limitada / 100.0) * 100).toInt()).coerceIn(META_MIN, META_MAX)
  }

  /** Racha: días consecutivos (desde el más reciente) en los que se alcanzó al menos el 60 % de la meta. */
  fun racha(historial: List<Int>, meta: Int): Int = historial.reversed().takeWhile { it >= meta * 0.6 }.count()
}

/** Lectura de pulso con su instante (ms desde epoch). */
data class Muestra(val ms: Long, val bpm: Int)
