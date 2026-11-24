package com.brasa.app

import com.brasa.app.domain.HeartRateSample
import com.brasa.app.domain.Rules
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

  @Test fun emotionFollowsProgress() {
    assertEquals(Emotion.DIM, Rules.emotion(0.1f, false, 0))
    assertEquals(Emotion.CALM, Rules.emotion(0.4f, false, 0))
    assertEquals(Emotion.HAPPY, Rules.emotion(0.75f, false, 0))
    assertEquals(Emotion.ECSTATIC, Rules.emotion(1.2f, false, 0))
  }

  @Test fun tensionHasPriorityAndInactivityDims() {
    assertEquals(Emotion.RESTLESS, Rules.emotion(1.5f, true, 0))
    assertEquals(Emotion.DIM, Rules.emotion(0.8f, false, 200))
  }

  @Test fun levelsAndStages() {
    assertEquals(1, Rules.level(0))
    assertEquals(5, Rules.level(400))
    assertEquals(Stage.FLAME, Rules.stage(400))
    assertEquals(Stage.BONFIRE, Rules.stage(2025))
    assertEquals(Stage.LEGEND, Rules.stage(9025))
    assertEquals(85, Rules.xpForSteps(8500))
  }

  @Test fun highHeartRateAtRestTriggersTension() {
    val currentTime = 10_000_000L
    val samples = listOf(HeartRateSample(currentTime - 120_000, 92), HeartRateSample(currentTime - 60_000, 95), HeartRateSample(currentTime, 97))
    assertTrue(Rules.isHeartRateElevatedAtRest(samples, base = 65, lastMovementMs = currentTime - 10 * 60_000, nowMs = currentTime))
  }

  @Test fun highHeartRateWhileMovingIsNotTension() {
    val currentTime = 10_000_000L
    val samples = listOf(HeartRateSample(currentTime - 60_000, 120), HeartRateSample(currentTime, 125))
    assertFalse(Rules.isHeartRateElevatedAtRest(samples, base = 65, lastMovementMs = currentTime - 60_000, nowMs = currentTime))
  }

  @Test fun normalHeartRateIsNotTension() {
    val currentTime = 10_000_000L
    val samples = listOf(HeartRateSample(currentTime - 60_000, 70), HeartRateSample(currentTime, 72))
    assertFalse(Rules.isHeartRateElevatedAtRest(samples, base = 65, lastMovementMs = 0, nowMs = currentTime))
  }

  @Test fun restingBaselineUsesPercentile20() {
    assertEquals(65, Rules.restingBaseline(listOf(80, 90)))
    assertEquals(62, Rules.restingBaseline(listOf(60, 61, 62, 64, 66, 70, 80, 90, 100, 110)))
  }

  @Test fun adaptiveGoalRisesWithLimit() {
    // Media 10.000 × 1,05 = 10.500, pero la meta actual es 8.000 → máximo +15 % = 9.200.
    assertEquals(9200, Rules.adaptiveGoal(List(7) { 10_000 }, 8000))
    // Media 8.000 × 1,05 = 8.400 (dentro del límite).
    assertEquals(8400, Rules.adaptiveGoal(List(7) { 8_000 }, 8000))
    // Sin historial se mantiene; nunca baja de 3.000.
    assertEquals(8000, Rules.adaptiveGoal(emptyList(), 8000))
    assertEquals(3000, Rules.adaptiveGoal(List(7) { 500 }, 3200))
  }

  @Test fun streakCountsRecentActiveDays() {
    assertEquals(2, Rules.streak(listOf(9000, 1000, 6000, 7000), 8000))
    assertEquals(0, Rules.streak(listOf(9000, 100), 8000))
  }

  @Test fun emotionAtBoundaries() {
    assertEquals(Emotion.DIM, Rules.emotion(0.199f, false, 0))
    assertEquals(Emotion.CALM, Rules.emotion(0.2f, false, 0))
    assertEquals(Emotion.HAPPY, Rules.emotion(0.6f, false, 0))
    assertEquals(Emotion.ECSTATIC, Rules.emotion(1f, false, 0))
    // Meta cumplida pesa más que la inactividad; 179 min quieta aún no apaga.
    assertEquals(Emotion.ECSTATIC, Rules.emotion(1f, false, 500))
    assertEquals(Emotion.HAPPY, Rules.emotion(0.7f, false, Rules.MINUTES_IDLE_DIM - 1))
  }

  @Test fun xpAndProgressWithinLevel() {
    assertEquals(0, Rules.xpForLevel(1))
    assertEquals(400, Rules.xpForLevel(5))
    assertEquals(0f, Rules.levelProgress(400))
    assertEquals(0.5f, Rules.levelProgress(400 + (625 - 400) / 2), 0.01f)
    assertEquals(1, Rules.level(-50)) // nunca por debajo de 1
    assertEquals(0, Rules.xpForSteps(99))
  }

  @Test fun adaptiveGoalFallsWithLimit() {
    // Media 4.000 × 1,05 = 4.200, pero desde 8.000 solo puede bajar un 15 % → 6.800.
    assertEquals(6800, Rules.adaptiveGoal(List(7) { 4_000 }, 8000))
    // Los días sin datos (0) no cuentan; nunca pasa de 15.000.
    assertEquals(15000, Rules.adaptiveGoal(listOf(0, 0, 40_000), 14_000))
  }

  @Test fun tensionNeedsTwoRecentReadings() {
    val currentTime = 10_000_000L
    // Una sola lectura alta, o lecturas altas pero viejas (> 3 min), no bastan.
    assertFalse(Rules.isHeartRateElevatedAtRest(listOf(HeartRateSample(currentTime, 120)), 65, 0, currentTime))
    assertFalse(Rules.isHeartRateElevatedAtRest(listOf(HeartRateSample(currentTime - 400_000, 120), HeartRateSample(currentTime - 300_000, 120)), 65, 0, currentTime))
    // Justo en el umbral (base + 20) sí cuenta.
    assertTrue(Rules.isHeartRateElevatedAtRest(listOf(HeartRateSample(currentTime - 60_000, 85), HeartRateSample(currentTime, 85)), 65, 0, currentTime))
  }

  @Test fun goalIsCelebratedOnceWhenCrossing100() {
    assertTrue(Rules.reachedGoal(0.98f, 1.0f))
    assertFalse(Rules.reachedGoal(1.0f, 1.2f)) // ya estaba cumplida
    assertFalse(Rules.reachedGoal(0.5f, 0.9f))
  }

  @Test fun levelUpAndNewStage() {
    assertTrue(Rules.leveledUp(6, 7))
    assertFalse(Rules.leveledUp(7, 7))
    assertFalse(Rules.leveledUp(8, 7))
    assertEquals(null, Rules.newStage(6, 7))
    assertEquals(Stage.BONFIRE, Rules.newStage(9, 10))
    assertEquals(Stage.LEGEND, Rules.newStage(12, 22))
    assertEquals(null, Rules.newStage(12, 9)) // bajar no es una etapa nueva
  }

  @Test fun aggregateLevelsThatLeaveTheWatch() {
    assertEquals(com.brasa.app.data.ActivityLevel.LOW, com.brasa.app.data.levelOf(0.29f))
    assertEquals(com.brasa.app.data.ActivityLevel.MEDIUM, com.brasa.app.data.levelOf(0.3f))
    assertEquals(com.brasa.app.data.ActivityLevel.HIGH, com.brasa.app.data.levelOf(0.8f))
    assertEquals(com.brasa.app.data.ActivityLevel.HIGH, com.brasa.app.data.levelOf(3f))
  }
}

class HistoryTest {
  @Test fun dayChangeClosesDayAndRecalculatesGoal() {
    val r = com.brasa.app.data.UserRecord(com.brasa.app.data.Day(epochDay = 100, steps = 9000), historial = List(6) { 8000 }, goal = 8000)
    val n = com.brasa.app.data.HistoryStore.rollOverDay(r, 101)
    assertEquals(101, n.today.epochDay)
    assertEquals(0, n.today.steps)
    assertEquals(9000, n.historial.last())
    assertEquals(8000, n.previousGoal)
    assertEquals(8600, n.goal) // media 8143 × 1,05 = 8550 → 8600
    assertEquals(90, n.previousXp)
  }

  @Test fun backupKeepsLevelAndGoalWithoutHealthData() {
    val r = com.brasa.app.data.UserRecord(
      com.brasa.app.data.Day(epochDay = 100, steps = 6000, heartRates = listOf(com.brasa.app.domain.HeartRateSample(1L, 70))),
      historial = List(6) { 8000 }, goal = 8200, previousGoal = 8000, previousXp = 1500, totalBreaths = 3, permissionsAsked = true,
    )
    val p = com.brasa.app.data.HistoryStore.progressOf(r)
    assertEquals(1500 + 60 + 30, p.totalXp)
    val newValue = com.brasa.app.data.HistoryStore.fromBackup(p, 105)
    // Mismo nivel y XP en el reloj nuevo.
    val newXp = newValue.previousXp + Rules.xpForSteps(newValue.today.steps) + newValue.totalBreaths * Rules.XP_PER_BREATH
    assertEquals(p.totalXp, newXp)
    assertEquals(Rules.level(p.totalXp), Rules.level(newXp))
    assertEquals(8200, newValue.goal)
    // Nada de salud ni permisos: el día empieza vacío y se vuelven a pedir los permisos.
    assertEquals(0, newValue.today.steps)
    assertTrue(newValue.today.heartRates.isEmpty())
    assertTrue(newValue.historial.isEmpty())
    assertFalse(newValue.permissionsAsked)
  }

  @Test fun withoutBackupStartsFromZero() {
    val r = com.brasa.app.data.HistoryStore.fromBackup(null, 105)
    assertEquals(0, r.previousXp)
    assertEquals(Rules.GOAL_INITIAL, r.goal)
  }

  @Test fun sameDayDoesNotChange() {
    val r = com.brasa.app.data.UserRecord(com.brasa.app.data.Day(epochDay = 100, steps = 500))
    assertEquals(r, com.brasa.app.data.HistoryStore.rollOverDay(r, 100))
  }
}

class DemoTest {
  @Test fun demoNeverDrawsFutureHoursAndMatchesTotal() {
    for (timeText in 0..23) {
      val perHour = demoStepsPerHour(6200, timeText)
      org.junit.Assert.assertTrue("hora $timeText", perHour.drop(timeText + 1).all { it == 0 })
      org.junit.Assert.assertTrue("hora $timeText: total ${perHour.sum()}", perHour.sum() in 6200 - 24..6200)
    }
  }

  @Test fun demoHeartRateOnlyReachesNow() {
    val start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val p = demoHeartRates(10 * 60)
    assertEquals(10 * 60 * 60_000L, p.last().ms - start)
    assertTrue(demoBreaths(10 * 60).isEmpty()) // la respiración de las 16:25 aún no ha pasado
    assertEquals(1, demoBreaths(20 * 60).size)
  }
}

class HeartRateTest {
  @Test fun oneReadingPerMinuteAndSorted() {
    var p = emptyList<com.brasa.app.domain.HeartRateSample>()
    for ((ms, bpm) in listOf(200_000L to 70, 60_000L to 65, 230_000L to 72, 400_000L to 80)) p = agregarPulso(p, com.brasa.app.domain.HeartRateSample(ms, bpm))
    assertEquals(listOf(60_000L, 230_000L, 400_000L), p.map { it.ms }) // 200 s y 230 s caen en el mismo minuto
    assertEquals(72, p[1].bpm) // la más nueva sustituye
  }
}
