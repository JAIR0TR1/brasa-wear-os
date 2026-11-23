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
import com.brasa.app.data.Ajustes
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.pantallas.AccionesAjustes
import com.brasa.app.ui.pantallas.AjustesPantalla
import com.brasa.app.ui.pantallas.FaseRespiro
import com.brasa.app.ui.pantallas.IndicadorPaginas
import com.brasa.app.ui.pantallas.Inicio
import com.brasa.app.ui.pantallas.Respirar
import com.brasa.app.ui.pantallas.dibujarArranque
import com.brasa.app.ui.theme.BrasaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

fun estadoDe(e: Emocion, etapa: Etapa = Etapa.LLAMA): EstadoBrasa {
  val progreso = when (e) {
    Emocion.APAGADA -> 0.12f; Emocion.INQUIETA -> 0.45f; Emocion.TRANQUILA -> 0.42f
    Emocion.FELIZ -> 0.73f; Emocion.EUFORICA -> 1f
  }
  return EstadoBrasa(
    emocion = e, etapa = etapa, nivel = 7, xp = 780, pasos = (8500 * progreso).toInt(), meta = 8500,
    progreso = progreso, pulso = 96, frase = 0, saludando = false,
  )
}

/** Pantallas a capturar en la fase 2. */
val pantallasFase2: List<String> = listOf("arranque-1-chispa", "arranque-2-remolino", "arranque-3-colapso", "arranque-4-destello") +
  Emocion.entries.map { "inicio-${it.name.lowercase()}" } +
  listOf("inicio-saludo", "respirar-inhala", "respirar-resultado", "ajustes", "permisos", "inicio-manual")

@Composable
fun ContenidoFase2(nombre: String) {
  when {
    nombre.startsWith("arranque") -> {
      val p = mapOf("1" to 0.12f, "2" to 0.5f, "3" to 0.78f, "4" to 0.9f)[nombre.substringAfter("arranque-").take(1)]!!
      Canvas(Modifier.fillMaxSize().background(Color.Black)) { dibujarArranque(p) }
    }
    nombre == "inicio-manual" -> Inicio(estadoDe(Emocion.APAGADA).copy(pasos = 0, progreso = 0f, fuente = Fuente.MANUAL), {}, {}, entrada = false, tiempoFijo = 1.3f, hora = HORA_TEXTO_CAPTURAS)
    nombre == "inicio-saludo" -> Inicio(estadoDe(Emocion.FELIZ).copy(saludando = true, frase = 1), {}, {}, entrada = false, tiempoFijo = 1.3f, hora = HORA_TEXTO_CAPTURAS)
    nombre.startsWith("inicio-") -> Box(Modifier.fillMaxSize()) {
      val e = Emocion.valueOf(nombre.substringAfter("inicio-").uppercase())
      Inicio(estadoDe(e), {}, {}, entrada = false, tiempoFijo = 1.3f, hora = HORA_TEXTO_CAPTURAS)
      IndicadorPaginas(total = 2, actual = 0)
    }
    nombre == "respirar-inhala" -> Respirar(Etapa.LLAMA, 96, { it - 14 }, { _, _ -> }, {}, faseFija = FaseRespiro.INHALA, tiempoFijo = 1.3f)
    nombre == "respirar-resultado" -> Respirar(Etapa.LLAMA, 96, { it - 14 }, { _, _ -> }, {}, faseFija = FaseRespiro.FIN, tiempoFijo = 1.3f)
    nombre == "permisos" -> com.brasa.app.ui.pantallas.Permisos({}, {}, tiempoFijo = 1.3f)
    else -> AjustesPantalla(Ajustes(emocionDemo = Emocion.INQUIETA), AccionesAjustes({}, {}, {}, {}), idiomaActual = "")
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CapturasFase2(private val pantalla: Int, private val variante: Variante) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun casos(): List<Array<Any>> {
      val todas = pantallasFase2.indices.map { arrayOf<Any>(it, redondoGrande) }
      val clave = listOf("inicio-feliz", "inicio-inquieta", "respirar-inhala", "respirar-resultado", "ajustes", "permisos")
        .map { pantallasFase2.indexOf(it) }
      return todas + clave.flatMap { p -> variantes.drop(1).map { arrayOf<Any>(p, it) } }
    }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun captura() {
    RuntimeEnvironment.setQualifiers(variante.calificadores)
    RuntimeEnvironment.setFontScale(variante.escalaFuente)
    val nombre = pantallasFase2[pantalla]
    compose.setContent {
      BrasaTheme(oscuro = variante.oscuro) {
        Box(
          Modifier.fillMaxSize().background(Color(0xFF141414))
            .then(if (variante.redondo) Modifier.clip(CircleShape) else Modifier),
        ) { ContenidoFase2(nombre) }
      }
    }
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/fase2/${variante.nombre}/$nombre.png")
  }
}
