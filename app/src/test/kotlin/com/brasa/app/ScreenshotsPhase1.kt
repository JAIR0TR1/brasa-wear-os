package com.brasa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.brasa.app.ui.screens.GalleryContent
import com.brasa.app.ui.screens.PageIndicator
import com.brasa.app.ui.screens.galleryPages
import com.brasa.app.ui.theme.BrasaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Variante de pantalla para la revisión visual. */
data class Variant(
  val name: String,
  val qualifiers: String,
  val round: Boolean = true,
  val dark: Boolean = true,
  val fontScale: Float = 1f,
)

val largeRound = Variant("redondo-grande", "es-w227dp-h227dp-round-watch-xhdpi")
val variants = listOf(
  largeRound,
  Variant("redondo-pequeno", "es-w192dp-h192dp-round-watch-xhdpi"),
  Variant("cuadrado", "es-w198dp-h198dp-notround-watch-xhdpi", round = false),
  Variant("claro", "es-w227dp-h227dp-round-watch-xhdpi", dark = false),
  Variant("ingles", "en-w227dp-h227dp-round-watch-xhdpi"),
  Variant("fuente-grande", "es-w227dp-h227dp-round-watch-xhdpi", fontScale = 1.3f),
)

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ScreenshotsPhase1(private val page: Int, private val variant: Variant) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun cases(): List<Array<Any>> {
      val allCases = galleryPages.indices.map { arrayOf<Any>(it, largeRound) }
      // Variantes en una muestra representativa: inquieta, feliz y leyenda.
      val sample = listOf(1, 3, 8).flatMap { p -> variants.drop(1).map { arrayOf<Any>(p, it) } }
      return allCases + sample
    }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun capture() {
    RuntimeEnvironment.setQualifiers(variant.qualifiers)
    RuntimeEnvironment.setFontScale(variant.fontScale)
    val p = galleryPages[page]
    compose.setContent {
      BrasaTheme(dark = variant.dark) {
        Box(
          Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .then(if (variant.round) Modifier.clip(CircleShape) else Modifier),
        ) {
          GalleryContent(p, fixedTime = 1.3f)
          PageIndicator(total = galleryPages.size, current = page)
        }
      }
    }
    val name = (if (p.isStage) "etapa-${p.stage.key}" else "emocion-${p.emotion.key}").lowercase()
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/fase1/${variant.name}/$name.png")
  }
}
