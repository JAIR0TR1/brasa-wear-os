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
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.pantallas.SubidaNivel
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
val pantallasV1 = listOf("subida-nivel-estallido", "subida-nivel-etapa", "subida-nivel")

@Composable
fun ContenidoV1(nombre: String) {
  when (nombre) {
    "subida-nivel-estallido" -> SubidaNivel(10, Etapa.FOGATA, etapaNueva = true, onFin = {}, progresoFijo = 0.22f)
    "subida-nivel-etapa" -> SubidaNivel(10, Etapa.FOGATA, etapaNueva = true, onFin = {}, progresoFijo = 0.6f)
    else -> SubidaNivel(8, Etapa.LLAMA, etapaNueva = false, onFin = {}, progresoFijo = 0.6f)
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CapturasV1(private val pantalla: Int, private val variante: Variante) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun casos(): List<Array<Any>> = pantallasV1.indices.flatMap { p -> variantes.map { arrayOf<Any>(p, it) } }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun captura() {
    RuntimeEnvironment.setQualifiers(variante.calificadores)
    RuntimeEnvironment.setFontScale(variante.escalaFuente)
    val nombre = pantallasV1[pantalla]
    compose.setContent {
      BrasaTheme(oscuro = variante.oscuro) {
        Box(
          Modifier.fillMaxSize().background(Color(0xFF141414))
            .then(if (variante.redondo) Modifier.clip(CircleShape) else Modifier),
        ) { ContenidoV1(nombre) }
      }
    }
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/v1/${variante.nombre}/$nombre.png")
  }
}
