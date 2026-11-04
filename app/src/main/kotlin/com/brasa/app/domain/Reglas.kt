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
}
