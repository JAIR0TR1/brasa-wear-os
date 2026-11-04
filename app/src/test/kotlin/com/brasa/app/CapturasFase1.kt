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
import com.brasa.app.ui.pantallas.ContenidoGaleria
import com.brasa.app.ui.pantallas.IndicadorPaginas
import com.brasa.app.ui.pantallas.paginasGaleria
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
data class Variante(
  val nombre: String,
  val calificadores: String,
  val redondo: Boolean = true,
  val oscuro: Boolean = true,
  val escalaFuente: Float = 1f,
)

val redondoGrande = Variante("redondo-grande", "es-w227dp-h227dp-round-watch-xhdpi")
val variantes = listOf(
  redondoGrande,
  Variante("redondo-pequeno", "es-w192dp-h192dp-round-watch-xhdpi"),
  Variante("cuadrado", "es-w198dp-h198dp-notround-watch-xhdpi", redondo = false),
  Variante("claro", "es-w227dp-h227dp-round-watch-xhdpi", oscuro = false),
  Variante("ingles", "en-w227dp-h227dp-round-watch-xhdpi"),
  Variante("fuente-grande", "es-w227dp-h227dp-round-watch-xhdpi", escalaFuente = 1.3f),
)

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CapturasFase1(private val pagina: Int, private val variante: Variante) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun casos(): List<Array<Any>> {
      val todas = paginasGaleria.indices.map { arrayOf<Any>(it, redondoGrande) }
      // Variantes en una muestra representativa: inquieta, feliz y leyenda.
      val muestra = listOf(1, 3, 8).flatMap { p -> variantes.drop(1).map { arrayOf<Any>(p, it) } }
      return todas + muestra
    }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun captura() {
    RuntimeEnvironment.setQualifiers(variante.calificadores)
    RuntimeEnvironment.setFontScale(variante.escalaFuente)
    val p = paginasGaleria[pagina]
    compose.setContent {
      BrasaTheme(oscuro = variante.oscuro) {
        Box(
          Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .then(if (variante.redondo) Modifier.clip(CircleShape) else Modifier),
        ) {
          ContenidoGaleria(p, tiempoFijo = 1.3f)
          IndicadorPaginas(total = paginasGaleria.size, actual = pagina)
        }
      }
    }
    val nombre = (if (p.esEtapa) "etapa-${p.etapa}" else "emocion-${p.emocion}").lowercase()
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/fase1/${variante.nombre}/$nombre.png")
  }
}
