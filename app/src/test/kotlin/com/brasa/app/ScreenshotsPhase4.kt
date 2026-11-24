package com.brasa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.brasa.app.data.AppSettings
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.screens.Evolution
import com.brasa.app.ui.screens.StepsChart
import com.brasa.app.ui.screens.HeartRateChart
import com.brasa.app.ui.screens.WeekChart
import com.brasa.app.ui.screens.PageIndicator
import com.brasa.app.ui.theme.BrasaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

val phase4Screens = listOf(
  "grafica-pasos", "grafica-pulso", "grafica-semana", "grafica-vacia-pasos", "grafica-vacia-pulso",
  "evolucion-chispa", "evolucion-llama", "evolucion-fogata", "evolucion-leyenda",
  "grafica-midiendo-pulso", "grafica-semana-primer-dia",
)

/** Fecha y hora fijas: las capturas no pueden depender del día en que se ejecutan. */
val CAPTURE_DATE: java.time.LocalDate = java.time.LocalDate.of(2026, 11, 4)
/** 18:30: la hora que se ve en Inicio y el límite de los datos demo de las gráficas coinciden. */
const val CAPTURE_MINUTE = 18 * 60 + 30
const val CAPTURE_HOUR = CAPTURE_MINUTE / 60
const val CAPTURE_HOUR_TEXT = "18:30"

@Composable
fun Phase4Content(name: String) {
  val demo = demoState(AppSettings(demo = true), 0, false, CAPTURE_MINUTE)
  val empty = demo.copy(steps = 0, stepsPerHour = List(24) { 0 }, heartRates = emptyList(), breathSessions = emptyList())
  val page = when {
    name.contains("pasos") -> 1; name.contains("pulso") -> 2; name.contains("semana") -> 3; else -> 4
  }
  Box(Modifier.fillMaxSize()) {
    when (name) {
      "grafica-pasos" -> StepsChart(demo.copy(stepsPerHour = demoStepsPerHour(demo.steps, CAPTURE_HOUR)), currentHour = CAPTURE_HOUR, fixed = true)
      "grafica-pulso" -> HeartRateChart(demo, fixed = true)
      "grafica-semana" -> WeekChart(demo, fixed = true, today = CAPTURE_DATE)
      "grafica-semana-primer-dia" -> WeekChart(demo.copy(historial = emptyList(), steps = 2300, goal = 6000, previousGoal = 6000), fixed = true, today = CAPTURE_DATE)
      "grafica-midiendo-pulso" -> HeartRateChart(demo.copy(heartRates = demo.heartRates.takeLast(2), breathSessions = emptyList()), fixed = true)
      "grafica-vacia-pasos" -> StepsChart(empty, currentHour = 9, fixed = true)
      "grafica-vacia-pulso" -> HeartRateChart(empty, fixed = true)
      else -> {
        val stage = Stage.entries.first { it.key == name.substringAfter("evolucion-").uppercase() }
        Evolution(demoState(AppSettings(demo = true, demoStage = stage, demoEmotion = Emotion.HAPPY), 0, false, CAPTURE_MINUTE), fixedTime = 1.3f)
      }
    }
    PageIndicator(total = 6, current = page)
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotsPhase4(private val screen: Int, private val variant: Variant) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun cases(): List<Array<Any>> {
      val allCases = phase4Screens.indices.map { arrayOf<Any>(it, largeRound) }
      val key = listOf("grafica-pasos", "grafica-pulso", "grafica-semana", "evolucion-fogata", "grafica-midiendo-pulso", "grafica-semana-primer-dia").map { phase4Screens.indexOf(it) }
      return allCases + key.flatMap { p -> variants.drop(1).map { arrayOf<Any>(p, it) } }
    }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun capture() {
    RuntimeEnvironment.setQualifiers(variant.qualifiers)
    RuntimeEnvironment.setFontScale(variant.fontScale)
    val name = phase4Screens[screen]
    compose.setContent {
      BrasaTheme(dark = variant.dark) {
        Box(
          Modifier.fillMaxSize().background(Color(0xFF141414))
            .then(if (variant.round) Modifier.clip(CircleShape) else Modifier),
        ) { Phase4Content(name) }
      }
    }
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/fase4/${variant.name}/$name.png")
  }
}
