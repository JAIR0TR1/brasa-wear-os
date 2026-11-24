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
import com.brasa.app.data.AppSettings
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.MainPages
import com.brasa.app.ui.screens.SettingsActions
import com.brasa.app.ui.screens.SettingsScreen
import com.brasa.app.ui.screens.Home
import com.brasa.app.ui.screens.Permissions
import com.brasa.app.ui.screens.Breathe
import com.brasa.app.ui.screens.TOTAL_SECONDS
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
class UiTest {
  @get:Rule val compose = createComposeRule()

  /** "Reducir animaciones" del sistema: Brasa queda quieta y la interfaz llega a reposo (también prueba ese modo). */
  @org.junit.Before fun disableAnimations() {
    android.provider.Settings.Global.putFloat(
      androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f,
    )
  }

  private val demo = demoState(AppSettings(demo = true), 0, false, CAPTURE_MINUTE)

  private fun pages(state: BrasaState = demo, actions: SettingsActions = SettingsActions({}, {}, {}, {})) {
    compose.setContent {
      BrasaTheme { MainPages(state, AppSettings(demo = true), actions, onTapBrasa = {}, onBreathe = {}) }
    }
  }

  @Test fun swipingVisitsTheSixPagesInOrder() {
    pages()
    compose.onNode(hasContentDescription("Página 1 de 6")).assertExists()
    val expected = listOf("Pasos por hora", "Pulso de hoy", "Semana:", "Etapa Llama", "Ajustes")
    for ((i, text) in expected.withIndex()) {
      compose.onRoot().performTouchInput { swipeLeft() }
      compose.waitForIdle()
      compose.onNode(hasContentDescription("Página ${i + 2} de 6")).assertExists()
      compose.onNode(hasContentDescription(text, substring = true) or hasText(text, substring = true)).assertExists()
    }
    // Volver atrás.
    compose.onRoot().performTouchInput { swipeRight() }
    compose.waitForIdle()
    compose.onNode(hasContentDescription("Página 5 de 6")).assertExists()
  }

  @Test fun settingsFromPagerCallTheActions() {
    var theme: Boolean? = null
    compose.setContent {
      BrasaTheme {
        MainPages(demo, AppSettings(demo = true), SettingsActions(theme = { theme = it }, {}, {}, {}), {}, {}, initialPage = 5)
      }
    }
    compose.onNodeWithText("Tema").performClick()
    assertEquals(false, theme) // estaba en oscuro → pide claro
  }

  @Test fun homeTapBrasaAndBreatheButton() {
    var taps = 0
    var breatheCount = 0
    compose.setContent {
      BrasaTheme { Home(testState(Emotion.RESTLESS), onTapBrasa = { taps++ }, onBreathe = { breatheCount++ }, entrance = false, fixedTime = 1f, timeText = CAPTURE_HOUR_TEXT) }
    }
    compose.onNode(hasContentDescription("Brasa, inquieta", substring = true)).performClick()
    assertEquals(1, taps)
    compose.onNodeWithText("Respirar").assertIsDisplayed().performClick()
    assertEquals(1, breatheCount)
  }

  @Test fun homeWithoutTensionHidesBreathe() {
    compose.setContent { BrasaTheme { Home(testState(Emotion.HAPPY), {}, {}, entrance = false, fixedTime = 1f, timeText = CAPTURE_HOUR_TEXT) } }
    compose.onNodeWithText("Respirar").assertDoesNotExist()
  }

  @Test fun permissionsAllowAndNotNow() {
    var allow = 0
    var notNow = 0
    compose.setContent { BrasaTheme { Permissions(onAllow = { allow++ }, onNotNow = { notNow++ }, fixedTime = 1f) } }
    compose.onNodeWithText("Permitir").performClick()
    compose.onNodeWithText("Ahora no").performClick()
    assertEquals(1, allow)
    assertEquals(1, notNow)
  }

  @Test fun breatheLastsOneMinuteAndShowsResult() {
    var result: Pair<Int?, Int?>? = null
    var ready = 0
    compose.mainClock.autoAdvance = false
    compose.setContent {
      BrasaTheme {
        Breathe(Stage.FLAME, heartRateBefore = 96, heartRateAfter = { it - 14 }, onCompleted = { a, d -> result = a to d }, onReady = { ready++ })
      }
    }
    compose.onNodeWithText("Prepárate").assertExists()
    compose.mainClock.advanceTimeBy(1_500L + 2_000L)
    compose.onNodeWithText("Inhala").assertExists()
    compose.mainClock.advanceTimeBy(TOTAL_SECONDS * 1000L)
    compose.mainClock.advanceTimeBy(1_000L)
    assertEquals(96 to 82, result)
    compose.onNodeWithText("−14").assertExists()
    compose.onNodeWithText("Listo").performClick()
    assertEquals(1, ready)
  }

  @Test fun settingsChangeThemeDemoEmotionAndStage() {
    var theme: Boolean? = null
    var demoEnabled: Boolean? = null
    var emotion: Emotion? = Emotion.HAPPY
    var stage: Stage? = null
    val actions = SettingsActions(theme = { theme = it }, demo = { demoEnabled = it }, emotion = { emotion = it }, stage = { stage = it })
    compose.setContent { BrasaTheme { SettingsScreen(AppSettings(demo = true), actions, currentLanguage = "es") } }
    compose.onNodeWithText("Tema").performClick()
    compose.onNodeWithText("Modo demo").performClick()
    compose.onNodeWithText("Emoción").performClick()
    compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(hasText("Etapa"))
    compose.onNodeWithText("Etapa").performClick()
    assertEquals(false, theme)
    assertEquals(false, demoEnabled)
    assertEquals(Emotion.DIM, emotion) // Automática → primera emoción
    assertEquals(Stage.SPARK, stage)
  }

  @Test fun settingsWithoutDemoHideEmotionAndStage() {
    compose.setContent { BrasaTheme { SettingsScreen(AppSettings(demo = false), SettingsActions({}, {}, {}, {}), currentLanguage = "es") } }
    compose.onNodeWithText("Emoción").assertDoesNotExist()
    compose.onNodeWithText("Etapa").assertDoesNotExist()
    assertTrue(compose.onAllNodes(hasText("Sensores de salud")).fetchSemanticsNodes().isNotEmpty())
  }
}
