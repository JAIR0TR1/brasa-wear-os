package com.brasa.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.brasa.app.data.Ajustes
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.PaginasPrincipales
import com.brasa.app.ui.pantallas.AccionesAjustes
import com.brasa.app.ui.pantallas.AjustesPantalla
import com.brasa.app.ui.pantallas.Inicio
import com.brasa.app.ui.pantallas.Permisos
import com.brasa.app.ui.pantallas.Respirar
import com.brasa.app.ui.pantallas.TOTAL_S
import com.brasa.app.ui.theme.BrasaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Pruebas de interfaz: cada toque y gesto hace lo que dice. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "es-w227dp-h227dp-round-watch-xhdpi")
class InterfazTest {
  @get:Rule val compose = createComposeRule()

  /** "Reducir animaciones" del sistema: Brasa queda quieta y la interfaz llega a reposo (también prueba ese modo). */
  @org.junit.Before fun reducirAnimaciones() {
    android.provider.Settings.Global.putFloat(
      androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f,
    )
  }

  private val demo = estadoDemo(Ajustes(demo = true), 0, false, MINUTO_CAPTURAS)

  private fun paginas(estado: EstadoBrasa = demo, acciones: AccionesAjustes = AccionesAjustes({}, {}, {}, {})) {
    compose.setContent {
      BrasaTheme { PaginasPrincipales(estado, Ajustes(demo = true), acciones, onTocarBrasa = {}, onRespirar = {}) }
    }
  }

  @Test fun deslizarRecorreLasSeisPaginasEnOrden() {
    paginas()
    compose.onNode(hasContentDescription("Página 1 de 6")).assertExists()
    val esperado = listOf("Pasos por hora", "Pulso de hoy", "Semana:", "Etapa Llama", "Ajustes")
    for ((i, texto) in esperado.withIndex()) {
      compose.onRoot().performTouchInput { swipeLeft() }
      compose.waitForIdle()
      compose.onNode(hasContentDescription("Página ${i + 2} de 6")).assertExists()
      compose.onNode(hasContentDescription(texto, substring = true) or hasText(texto, substring = true)).assertExists()
    }
    // Volver atrás.
    compose.onRoot().performTouchInput { swipeRight() }
    compose.waitForIdle()
    compose.onNode(hasContentDescription("Página 5 de 6")).assertExists()
  }

  @Test fun ajustesDesdeElPaginadorLlamanALasAcciones() {
    var tema: Boolean? = null
    compose.setContent {
      BrasaTheme {
        PaginasPrincipales(demo, Ajustes(demo = true), AccionesAjustes(tema = { tema = it }, {}, {}, {}), {}, {}, paginaInicial = 5)
      }
    }
    compose.onNodeWithText("Tema").performClick()
    assertEquals(false, tema) // estaba en oscuro → pide claro
  }

  @Test fun inicioTocarBrasaYBotonRespirar() {
    var toques = 0
    var respirar = 0
    compose.setContent {
      BrasaTheme { Inicio(estadoDe(Emocion.INQUIETA), onTocarBrasa = { toques++ }, onRespirar = { respirar++ }, entrada = false, tiempoFijo = 1f, hora = "10:42") }
    }
    compose.onNode(hasContentDescription("Brasa, inquieta", substring = true)).performClick()
    assertEquals(1, toques)
    compose.onNodeWithText("Respirar").assertIsDisplayed().performClick()
    assertEquals(1, respirar)
  }

  @Test fun inicioSinTensionNoMuestraRespirar() {
    compose.setContent { BrasaTheme { Inicio(estadoDe(Emocion.FELIZ), {}, {}, entrada = false, tiempoFijo = 1f, hora = "10:42") } }
    compose.onNodeWithText("Respirar").assertDoesNotExist()
  }

  @Test fun permisosPermitirYAhoraNo() {
    var permitir = 0
    var ahoraNo = 0
    compose.setContent { BrasaTheme { Permisos(onPermitir = { permitir++ }, onAhoraNo = { ahoraNo++ }, tiempoFijo = 1f) } }
    compose.onNodeWithText("Permitir").performClick()
    compose.onNodeWithText("Ahora no").performClick()
    assertEquals(1, permitir)
    assertEquals(1, ahoraNo)
  }

  @Test fun respirarDuraUnMinutoYMuestraElResultado() {
    var resultado: Pair<Int?, Int?>? = null
    var listo = 0
    compose.mainClock.autoAdvance = false
    compose.setContent {
      BrasaTheme {
        Respirar(Etapa.LLAMA, pulsoAntes = 96, pulsoDespues = { it - 14 }, onCompletado = { a, d -> resultado = a to d }, onListo = { listo++ })
      }
    }
    compose.onNodeWithText("Prepárate").assertExists()
    compose.mainClock.advanceTimeBy(1_500L + 2_000L)
    compose.onNodeWithText("Inhala").assertExists()
    compose.mainClock.advanceTimeBy(TOTAL_S * 1000L)
    compose.mainClock.advanceTimeBy(1_000L)
    assertEquals(96 to 82, resultado)
    compose.onNodeWithText("−14").assertExists()
    compose.onNodeWithText("Listo").performClick()
    assertEquals(1, listo)
  }

  @Test fun ajustesCambianTemaDemoEmocionYEtapa() {
    var tema: Boolean? = null
    var demoActivado: Boolean? = null
    var emocion: Emocion? = Emocion.FELIZ
    var etapa: Etapa? = null
    val acciones = AccionesAjustes(tema = { tema = it }, demo = { demoActivado = it }, emocion = { emocion = it }, etapa = { etapa = it })
    compose.setContent { BrasaTheme { AjustesPantalla(Ajustes(demo = true), acciones, idiomaActual = "es") } }
    compose.onNodeWithText("Tema").performClick()
    compose.onNodeWithText("Modo demo").performClick()
    compose.onNodeWithText("Emoción").performClick()
    compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(hasText("Etapa"))
    compose.onNodeWithText("Etapa").performClick()
    assertEquals(false, tema)
    assertEquals(false, demoActivado)
    assertEquals(Emocion.APAGADA, emocion) // Automática → primera emoción
    assertEquals(Etapa.CHISPA, etapa)
  }

  @Test fun ajustesSinDemoOcultanEmocionYEtapa() {
    compose.setContent { BrasaTheme { AjustesPantalla(Ajustes(demo = false), AccionesAjustes({}, {}, {}, {}), idiomaActual = "es") } }
    compose.onNodeWithText("Emoción").assertDoesNotExist()
    compose.onNodeWithText("Etapa").assertDoesNotExist()
    assertTrue(compose.onAllNodes(hasText("Sensores de salud")).fetchSemanticsNodes().isNotEmpty())
  }
}
