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
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.screens.LevelUp
import com.brasa.app.ui.theme.BrasaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Pantallas nuevas de la versión 1.0.0 (todas en las 6 variantes). */
val v1Screens = listOf("subida-nivel-estallido", "subida-nivel-etapa", "subida-nivel")

@Composable
fun V1Content(name: String) {
  when (name) {
    "subida-nivel-estallido" -> LevelUp(10, Stage.BONFIRE, newStage = true, onFinish = {}, fixedProgress = 0.22f)
    "subida-nivel-etapa" -> LevelUp(10, Stage.BONFIRE, newStage = true, onFinish = {}, fixedProgress = 0.6f)
    else -> LevelUp(8, Stage.FLAME, newStage = false, onFinish = {}, fixedProgress = 0.6f)
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotsV1(private val screen: Int, private val variant: Variant) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun cases(): List<Array<Any>> = v1Screens.indices.flatMap { p -> variants.map { arrayOf<Any>(p, it) } }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun capture() {
    RuntimeEnvironment.setQualifiers(variant.qualifiers)
    RuntimeEnvironment.setFontScale(variant.fontScale)
    val name = v1Screens[screen]
    compose.setContent {
      BrasaTheme(dark = variant.dark) {
        Box(
          Modifier.fillMaxSize().background(Color(0xFF141414))
            .then(if (variant.round) Modifier.clip(CircleShape) else Modifier),
        ) { V1Content(name) }
      }
    }
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/v1/${variant.name}/$name.png")
  }
}
