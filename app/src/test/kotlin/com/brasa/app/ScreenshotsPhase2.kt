package com.brasa.app

import androidx.compose.foundation.Canvas
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
import com.brasa.app.ui.screens.SettingsActions
import com.brasa.app.ui.screens.SettingsScreen
import com.brasa.app.ui.screens.BreathPhase
import com.brasa.app.ui.screens.PageIndicator
import com.brasa.app.ui.screens.Home
import com.brasa.app.ui.screens.Breathe
import com.brasa.app.ui.screens.drawStartup
import com.brasa.app.ui.theme.BrasaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

fun testState(e: Emotion, stage: Stage = Stage.FLAME): BrasaState {
  val progress = when (e) {
    Emotion.DIM -> 0.12f; Emotion.RESTLESS -> 0.45f; Emotion.CALM -> 0.42f
    Emotion.HAPPY -> 0.73f; Emotion.ECSTATIC -> 1f
  }
  return BrasaState(
    emotion = e, stage = stage, level = 7, xp = 780, steps = (8500 * progress).toInt(), goal = 8500,
    progress = progress, heartRate = 96, phrase = 0, greeting = false,
  )
}

/** Pantallas a capturar en la fase 2. */
val phase2Screens: List<String> = listOf("arranque-1-chispa", "arranque-2-remolino", "arranque-3-colapso", "arranque-4-destello") +
  Emotion.entries.map { "inicio-${it.key.lowercase()}" } +
  listOf("inicio-saludo", "respirar-inhala", "respirar-resultado", "ajustes", "permisos", "inicio-manual")

@Composable
fun Phase2Content(name: String) {
  when {
    name.startsWith("arranque") -> {
      val p = mapOf("1" to 0.12f, "2" to 0.5f, "3" to 0.78f, "4" to 0.9f)[name.substringAfter("arranque-").take(1)]!!
      Canvas(Modifier.fillMaxSize().background(Color.Black)) { drawStartup(p) }
    }
    name == "inicio-manual" -> Home(testState(Emotion.DIM).copy(steps = 0, progress = 0f, source = DataSource.MANUAL), {}, {}, entrance = false, fixedTime = 1.3f, timeText = CAPTURE_HOUR_TEXT)
    name == "inicio-saludo" -> Home(testState(Emotion.HAPPY).copy(greeting = true, phrase = 1), {}, {}, entrance = false, fixedTime = 1.3f, timeText = CAPTURE_HOUR_TEXT)
    name.startsWith("inicio-") -> Box(Modifier.fillMaxSize()) {
      val e = Emotion.entries.first { it.key == name.substringAfter("inicio-").uppercase() }
      Home(testState(e), {}, {}, entrance = false, fixedTime = 1.3f, timeText = CAPTURE_HOUR_TEXT)
      PageIndicator(total = 2, current = 0)
    }
    name == "respirar-inhala" -> Breathe(Stage.FLAME, 96, { it - 14 }, { _, _ -> }, {}, fixedPhase = BreathPhase.INHALE, fixedTime = 1.3f)
    name == "respirar-resultado" -> Breathe(Stage.FLAME, 96, { it - 14 }, { _, _ -> }, {}, fixedPhase = BreathPhase.DONE, fixedTime = 1.3f)
    name == "permisos" -> com.brasa.app.ui.screens.Permissions({}, {}, fixedTime = 1.3f)
    else -> SettingsScreen(AppSettings(demoEmotion = Emotion.RESTLESS), SettingsActions({}, {}, {}, {}), currentLanguage = "")
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotsPhase2(private val screen: Int, private val variant: Variant) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun cases(): List<Array<Any>> {
      val allCases = phase2Screens.indices.map { arrayOf<Any>(it, largeRound) }
      val key = listOf("inicio-feliz", "inicio-inquieta", "respirar-inhala", "respirar-resultado", "ajustes", "permisos")
        .map { phase2Screens.indexOf(it) }
      return allCases + key.flatMap { p -> variants.drop(1).map { arrayOf<Any>(p, it) } }
    }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun capture() {
    RuntimeEnvironment.setQualifiers(variant.qualifiers)
    RuntimeEnvironment.setFontScale(variant.fontScale)
    val name = phase2Screens[screen]
    compose.setContent {
      BrasaTheme(dark = variant.dark) {
        Box(
          Modifier.fillMaxSize().background(Color(0xFF141414))
            .then(if (variant.round) Modifier.clip(CircleShape) else Modifier),
        ) { Phase2Content(name) }
      }
    }
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/fase2/${variant.name}/$name.png")
  }
}
