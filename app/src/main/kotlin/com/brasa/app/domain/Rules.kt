package com.brasa.app.domain

import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Reglas deterministas de Brasa (puras, sin Android: se prueban con JUnit).
 * Documentadas en docs/direccion-de-arte.md y README.
 */
object Rules {
  /** Horas sin moverse a partir de las cuales Brasa se apaga aunque la meta vaya bien. */
  const val MINUTES_IDLE_DIM = 180

  /**
   * Emoción a partir del estado del día.
   * Prioridad: tensión (pulso elevado en reposo) > meta cumplida > inactividad/meta baja > progreso.
   */
  fun emotion(progress: Float, isHeartRateElevatedAtRest: Boolean, minutesIdle: Int): Emotion = when {
    isHeartRateElevatedAtRest -> Emotion.RESTLESS
    progress >= 1f -> Emotion.ECSTATIC
    progress < 0.2f || minutesIdle >= MINUTES_IDLE_DIM -> Emotion.DIM
    progress < 0.6f -> Emotion.CALM
    else -> Emotion.HAPPY
  }

  /** 1 XP por cada 100 pasos; 10 XP por respiración completada. */
  fun xpForSteps(steps: Int): Int = steps / 100
  const val XP_PER_BREATH = 10

  /** Nivel = 1 + ⌊√(xp/25)⌋ → nivel 5 ≈ 400 XP (≈ 1 semana activa), 10 ≈ 2025 XP (≈ 1 mes), 20 ≈ 9025 XP. */
  fun level(xp: Int): Int = 1 + floor(sqrt(xp.coerceAtLeast(0) / 25.0)).toInt()

  /** XP necesaria para alcanzar un nivel. */
  fun xpForLevel(level: Int): Int = 25 * (level - 1) * (level - 1)

  /** Progreso 0..1 dentro del nivel actual. */
  fun levelProgress(xp: Int): Float {
    val n = level(xp)
    val a = xpForLevel(n)
    val b = xpForLevel(n + 1)
    return ((xp - a).toFloat() / (b - a)).coerceIn(0f, 1f)
  }

  fun stage(xp: Int): Stage = Stage.fromLevel(level(xp))

  // ---------- Tensión (señales, no diagnóstico) ----------

  /** Lpm por encima de la base en reposo que cuentan como "pulso elevado". */
  const val TENSION_MARGIN_BPM = 20
  /** Ventana de pulso que se promedia. */
  const val TENSION_WINDOW_MS = 3 * 60_000L
  /** Sin pasos durante este tiempo = "en reposo". */
  const val REST_MS = 5 * 60_000L
  const val DEFAULT_BASELINE = 65

  /** Base en reposo: percentil 20 de las muestras del día (robusto a picos). Con pocas muestras, 65 lpm. */
  fun restingBaseline(bpm: List<Int>): Int {
    if (bpm.size < 10) return DEFAULT_BASELINE
    val sorted = bpm.sorted()
    return sorted[(sorted.size * 0.2).toInt()]
  }

  /**
   * Pulso elevado en reposo: la media de los últimos 3 min supera base + 20 lpm (con al menos 2 lecturas)
   * y no ha habido movimiento en 5 min. Si te estás moviendo, un pulso alto es normal.
   */
  fun isHeartRateElevatedAtRest(samples: List<HeartRateSample>, base: Int, lastMovementMs: Long, nowMs: Long): Boolean {
    if (nowMs - lastMovementMs < REST_MS) return false
    val recent = samples.filter { nowMs - it.ms <= TENSION_WINDOW_MS }
    if (recent.size < 2) return false
    return recent.map { it.bpm }.average() >= base + TENSION_MARGIN_BPM
  }

  // ---------- Meta adaptativa ----------

  const val GOAL_MIN = 3000
  const val GOAL_MAX = 15000
  const val GOAL_INITIAL = 6000
  /** Cambio máximo de la meta de un día a otro (±15 %). */
  const val MAX_CHANGE = 0.15

  /**
   * Meta de mañana = media de los últimos 7 días × 1,05 (un pequeño reto), redondeada a 100,
   * limitada a ±15 % de la meta actual y al rango 3.000–15.000. Sin historial: se mantiene.
   */
  fun adaptiveGoal(historial: List<Int>, currentGoal: Int): Int {
    val days = historial.takeLast(7).filter { it > 0 }
    if (days.isEmpty()) return currentGoal.coerceIn(GOAL_MIN, GOAL_MAX)
    val proposal = days.average() * 1.05
    val limited = proposal.coerceIn(currentGoal * (1 - MAX_CHANGE), currentGoal * (1 + MAX_CHANGE))
    return ((Math.round(limited / 100.0) * 100).toInt()).coerceIn(GOAL_MIN, GOAL_MAX)
  }

  // ---------- Celebraciones ----------

  /** La meta se celebra una vez: al cruzar el 100 % (no cada vez que se recalcula estando por encima). */
  fun reachedGoal(progressBefore: Float, progressNow: Float): Boolean = progressBefore < 1f && progressNow >= 1f

  fun leveledUp(levelBefore: Int, levelNow: Int): Boolean = levelNow > levelBefore

  /** Etapa nueva alcanzada al subir de nivel (null si sigue en la misma). */
  fun newStage(levelBefore: Int, levelNow: Int): Stage? =
    Stage.fromLevel(levelNow).takeIf { it != Stage.fromLevel(levelBefore) && levelNow > levelBefore }

  /** Racha: días consecutivos (desde el más reciente) en los que se alcanzó al menos el 60 % de la meta. */
  fun streak(historial: List<Int>, goal: Int): Int = historial.reversed().takeWhile { it >= goal * 0.6 }.count()
}

/** Lectura de pulso con su instante (ms desde epoch). */
@kotlinx.serialization.Serializable
data class HeartRateSample(val ms: Long, val bpm: Int)
