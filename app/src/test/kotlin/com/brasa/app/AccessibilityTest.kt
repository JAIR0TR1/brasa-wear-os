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
import com.brasa.app.data.AppSettings
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.screens.SettingsActions
import com.brasa.app.ui.screens.SettingsScreen
import com.brasa.app.ui.screens.Evolution
import com.brasa.app.ui.screens.BreathPhase
import com.brasa.app.ui.screens.StepsChart
import com.brasa.app.ui.screens.HeartRateChart
import com.brasa.app.ui.screens.WeekChart
import com.brasa.app.ui.screens.Home
import com.brasa.app.ui.screens.Permissions
import com.brasa.app.ui.screens.Breathe
import com.brasa.app.ui.screens.LevelUp
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
class AccessibilityTest {
  @get:Rule val compose = createComposeRule()

  private val demo = demoState(AppSettings(demo = true), 0, false, CAPTURE_MINUTE)

  private val screens: Map<String, @Composable () -> Unit> = mapOf(
    "inicio-inquieta" to { Home(testState(Emotion.RESTLESS), {}, {}, entrance = false, fixedTime = 1f, timeText = CAPTURE_HOUR_TEXT) },
    "inicio-manual" to { Home(testState(Emotion.DIM).copy(steps = 0, progress = 0f, source = DataSource.MANUAL), {}, {}, entrance = false, fixedTime = 1f, timeText = CAPTURE_HOUR_TEXT) },
    "permisos" to { Permissions({}, {}, fixedTime = 1f) },
    "ajustes" to { SettingsScreen(AppSettings(demo = true), SettingsActions({}, {}, {}, {}), currentLanguage = "es") },
    "respirar" to { Breathe(Stage.FLAME, 96, { it - 14 }, { _, _ -> }, {}, fixedPhase = BreathPhase.INHALE, fixedTime = 1f) },
    "resultado" to { Breathe(Stage.FLAME, 96, { it - 14 }, { _, _ -> }, {}, fixedPhase = BreathPhase.DONE, fixedTime = 1f) },
    "pasos" to { StepsChart(demo, currentHour = CAPTURE_HOUR, fixed = true) },
    "pulso" to { HeartRateChart(demo, fixed = true) },
    "semana" to { WeekChart(demo, fixed = true, today = CAPTURE_DATE) },
    "evolucion" to { Evolution(demo, fixedTime = 1f) },
    "subida-nivel" to { LevelUp(10, Stage.BONFIRE, newStage = true, onFinish = {}, fixedProgress = 0.6f) },
  )

  /** Recorre cada pantalla y comprueba [revisar] sobre su árbol semántico combinado. */
  private fun visitEachScreen(inspect: (String, List<SemanticsNode>) -> Unit) {
    var current by androidx.compose.runtime.mutableStateOf(screens.keys.first())
    compose.setContent { BrasaTheme { screens.getValue(current)() } }
    for (name in screens.keys) {
      current = name
      compose.waitForIdle()
      inspect(name, compose.onAllNodes(SemanticsMatcher("todo") { true }).fetchSemanticsNodes())
    }
  }

  private fun nameOf(n: SemanticsNode): String =
    (n.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
      n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }).joinToString(" ").trim()

  @Test fun allTouchTargetsAreAtLeast48dp() {
    val minimum = Tokens.Spacing.touchTarget.value
    visitEachScreen { screen, nodes ->
      val screenDensity = compose.density.density
      nodes.filter { SemanticsActions.OnClick in it.config }.forEach { n ->
        // Tamaño de layout (sin la escala visual de ScalingLazyColumn en los bordes de la lista).
        val width = n.size.width / screenDensity
        val height = n.size.height / screenDensity
        assertTrue("$screen: '${nameOf(n)}' mide ${width.toInt()}×${height.toInt()} dp", width >= minimum - 0.5f && height >= minimum - 0.5f)
      }
    }
  }

  @Test fun allTouchTargetsHaveTalkBackNames() {
    visitEachScreen { screen, _ ->
      compose.onAllNodes(hasClickAction(), useUnmergedTree = false).fetchSemanticsNodes().forEach { n ->
        val label = n.config.getOrNull(SemanticsActions.OnClick)?.label
        assertTrue("$screen: elemento táctil sin nombre", nameOf(n).isNotBlank() || !label.isNullOrBlank())
      }
    }
  }

  @Test fun brasaAndChartsHaveDescriptions() {
    visitEachScreen { screen, nodes ->
      val descriptions = nodes.flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
      val expected = when (screen) {
        "pasos" -> "Pasos por hora"
        "pulso" -> "Pulso de hoy"
        "semana" -> "Semana:"
        "evolucion" -> "Etapa"
        "subida-nivel" -> "¡Nivel 10!"
        "ajustes", "permisos" -> null
        else -> "Brasa,"
      } ?: return@visitEachScreen
      assertTrue("$screen sin descripción '$expected': $descriptions", descriptions.any { it.contains(expected) })
    }
  }

  @Test fun titlesAreHeadings() {
    visitEachScreen { screen, nodes ->
      if (screen !in setOf("pulso", "semana", "evolucion", "respirar", "ajustes")) return@visitEachScreen
      assertTrue("$screen sin encabezado", nodes.any { SemanticsProperties.Heading in it.config })
    }
  }

  // ---------------------------------------------------------------- Contraste WCAG

  private fun contrast(a: Color, b: Color): Double {
    val la = a.luminance() + 0.05
    val lb = b.luminance() + 0.05
    return maxOf(la, lb) / minOf(la, lb)
  }

  @Test fun textContrastMeetsAA() {
    val c = Tokens.Colors
    val pairs = buildList {
      add(Triple("texto/oscuro", c.TextPrimary, c.Black))
      add(Triple("secundario/oscuro", c.SecondaryText, c.Black))
      add(Triple("texto/claro", c.LightText, c.LightBackground))
      add(Triple("secundario/claro", c.LightSecondaryText, c.LightBackground))
      add(Triple("fuego/claro", c.LightThemeFire, c.LightBackground))
      add(Triple("dorado/oscuro", c.Gold, c.Black))
      add(Triple("calma/oscuro", c.Water, c.Black))
      add(Triple("calma/claro", c.LightThemeWater, c.LightBackground))
      for (e in Emotion.entries) {
        add(Triple("acento ${e.name}/oscuro", Tokens.palette(e, true).accent, c.Black))
        add(Triple("acento ${e.name}/claro", Tokens.palette(e, false).accent, c.LightBackground))
        // Texto de los botones sobre su color (negro en oscuro, blanco en claro).
        add(Triple("botón ${e.name}/oscuro", c.Black, Tokens.palette(e, true).accent))
        add(Triple("botón ${e.name}/claro", c.White, Tokens.palette(e, false).accent))
      }
    }
    val failures = pairs.map { (n, a, b) -> n to contrast(a, b) }.filter { it.second < 4.5 }
    assertTrue("Contraste < 4,5:1: ${failures.joinToString { "${it.first} ${"%.2f".format(it.second)}" }}", failures.isEmpty())
  }
}
