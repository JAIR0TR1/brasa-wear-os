package com.brasa.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import com.brasa.app.data.Ajustes
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.pantallas.AccionesAjustes
import com.brasa.app.ui.pantallas.AjustesPantalla
import com.brasa.app.ui.pantallas.Evolucion
import com.brasa.app.ui.pantallas.FaseRespiro
import com.brasa.app.ui.pantallas.GraficaPasos
import com.brasa.app.ui.pantallas.GraficaPulso
import com.brasa.app.ui.pantallas.GraficaSemana
import com.brasa.app.ui.pantallas.Inicio
import com.brasa.app.ui.pantallas.Permisos
import com.brasa.app.ui.pantallas.Respirar
import com.brasa.app.ui.pantallas.SubidaNivel
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.Tokens
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Accesibilidad automática: áreas táctiles ≥ 48 dp, todo lo táctil y toda imagen con nombre para TalkBack,
 * títulos como encabezados y contraste AA (≥ 4,5:1) de los colores de texto de los tokens.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w192dp-h192dp-round-watch-xhdpi")
class AccesibilidadTest {
  @get:Rule val compose = createComposeRule()

  private val demo = estadoDemo(Ajustes(demo = true), 0, false, MINUTO_CAPTURAS)

  private val pantallas: Map<String, @Composable () -> Unit> = mapOf(
    "inicio-inquieta" to { Inicio(estadoDe(Emocion.INQUIETA), {}, {}, entrada = false, tiempoFijo = 1f, hora = "10:42") },
    "inicio-manual" to { Inicio(estadoDe(Emocion.APAGADA).copy(pasos = 0, progreso = 0f, fuente = Fuente.MANUAL), {}, {}, entrada = false, tiempoFijo = 1f, hora = "10:42") },
    "permisos" to { Permisos({}, {}, tiempoFijo = 1f) },
    "ajustes" to { AjustesPantalla(Ajustes(demo = true), AccionesAjustes({}, {}, {}, {}), idiomaActual = "es") },
    "respirar" to { Respirar(Etapa.LLAMA, 96, { it - 14 }, { _, _ -> }, {}, faseFija = FaseRespiro.INHALA, tiempoFijo = 1f) },
    "resultado" to { Respirar(Etapa.LLAMA, 96, { it - 14 }, { _, _ -> }, {}, faseFija = FaseRespiro.FIN, tiempoFijo = 1f) },
    "pasos" to { GraficaPasos(demo, horaActual = 20, fijo = true) },
    "pulso" to { GraficaPulso(demo, fijo = true) },
    "semana" to { GraficaSemana(demo, fijo = true, hoy = HOY_CAPTURAS) },
    "evolucion" to { Evolucion(demo, tiempoFijo = 1f) },
    "subida-nivel" to { SubidaNivel(10, Etapa.FOGATA, etapaNueva = true, onFin = {}, progresoFijo = 0.6f) },
  )

  /** Recorre cada pantalla y comprueba [revisar] sobre su árbol semántico combinado. */
  private fun recorrer(revisar: (String, List<SemanticsNode>) -> Unit) {
    var actual by androidx.compose.runtime.mutableStateOf(pantallas.keys.first())
    compose.setContent { BrasaTheme { pantallas.getValue(actual)() } }
    for (nombre in pantallas.keys) {
      actual = nombre
      compose.waitForIdle()
      revisar(nombre, compose.onAllNodes(SemanticsMatcher("todo") { true }).fetchSemanticsNodes())
    }
  }

  private fun nombreDe(n: SemanticsNode): String =
    (n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
      n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }).joinToString(" ").trim()

  @Test fun todoLoTactilMideAlMenos48dp() {
    val minimo = Tokens.Espacio.tactil.value
    recorrer { pantalla, nodos ->
      val densidad = compose.density.density
      nodos.filter { SemanticsActions.OnClick in it.config }.forEach { n ->
        // Tamaño de layout (sin la escala visual de ScalingLazyColumn en los bordes de la lista).
        val ancho = n.size.width / densidad
        val alto = n.size.height / densidad
        assertTrue("$pantalla: '${nombreDe(n)}' mide ${ancho.toInt()}×${alto.toInt()} dp", ancho >= minimo - 0.5f && alto >= minimo - 0.5f)
      }
    }
  }

  @Test fun todoLoTactilTieneNombreParaTalkBack() {
    recorrer { pantalla, _ ->
      compose.onAllNodes(hasClickAction(), useUnmergedTree = false).fetchSemanticsNodes().forEach { n ->
        val etiqueta = n.config.getOrNull(SemanticsActions.OnClick)?.label
        assertTrue("$pantalla: elemento táctil sin nombre", nombreDe(n).isNotBlank() || !etiqueta.isNullOrBlank())
      }
    }
  }

  @Test fun brasaYLasGraficasTienenDescripcion() {
    recorrer { pantalla, nodos ->
      val descripciones = nodos.flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
      val esperada = when (pantalla) {
        "pasos" -> "Pasos por hora"
        "pulso" -> "Pulso de hoy"
        "semana" -> "Semana:"
        "evolucion" -> "Etapa"
        "subida-nivel" -> "¡Nivel 10!"
        "ajustes", "permisos" -> null
        else -> "Brasa,"
      } ?: return@recorrer
      assertTrue("$pantalla sin descripción '$esperada': $descripciones", descripciones.any { it.contains(esperada) })
    }
  }

  @Test fun losTitulosSonEncabezados() {
    recorrer { pantalla, nodos ->
      if (pantalla !in setOf("pulso", "semana", "evolucion", "respirar", "ajustes")) return@recorrer
      assertTrue("$pantalla sin encabezado", nodos.any { SemanticsProperties.Heading in it.config })
    }
  }

  // ---------------------------------------------------------------- Contraste WCAG

  private fun contraste(a: Color, b: Color): Double {
    val la = a.luminance() + 0.05
    val lb = b.luminance() + 0.05
    return maxOf(la, lb) / minOf(la, lb)
  }

  @Test fun contrasteAADeLosTextos() {
    val c = Tokens.Colores
    val pares = buildList {
      add(Triple("texto/oscuro", c.Texto, c.Negro))
      add(Triple("secundario/oscuro", c.TextoSecundario, c.Negro))
      add(Triple("texto/claro", c.TextoClaro, c.FondoClaro))
      add(Triple("secundario/claro", c.TextoSecundarioClaro, c.FondoClaro))
      add(Triple("fuego/claro", c.FuegoClaroTema, c.FondoClaro))
      add(Triple("dorado/oscuro", c.Dorado, c.Negro))
      add(Triple("calma/oscuro", c.Agua, c.Negro))
      add(Triple("calma/claro", c.AguaClaroTema, c.FondoClaro))
      for (e in Emocion.entries) {
        add(Triple("acento ${e.name}/oscuro", Tokens.paleta(e, true).acento, c.Negro))
        add(Triple("acento ${e.name}/claro", Tokens.paleta(e, false).acento, c.FondoClaro))
        // Texto de los botones sobre su color (negro en oscuro, blanco en claro).
        add(Triple("botón ${e.name}/oscuro", c.Negro, Tokens.paleta(e, true).acento))
        add(Triple("botón ${e.name}/claro", c.Blanco, Tokens.paleta(e, false).acento))
      }
    }
    val fallos = pares.map { (n, a, b) -> n to contraste(a, b) }.filter { it.second < 4.5 }
    assertTrue("Contraste < 4,5:1: ${fallos.joinToString { "${it.first} ${"%.2f".format(it.second)}" }}", fallos.isEmpty())
  }
}
