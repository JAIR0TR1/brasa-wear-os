package com.brasa.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import com.brasa.app.ui.screens.Evolution
import com.brasa.app.ui.screens.StepsChart
import com.brasa.app.ui.screens.HeartRateChart
import com.brasa.app.ui.screens.WeekChart
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.brasa.app.BrasaViewModel
import com.brasa.app.BrasaState
import com.brasa.app.data.AppSettings
import com.brasa.app.Celebration
import com.brasa.app.ui.screens.LevelUp
import com.brasa.app.data.healthPermissions
import com.brasa.app.ui.screens.Permissions
import com.brasa.app.ui.screens.SettingsActions
import com.brasa.app.ui.screens.SettingsScreen
import com.brasa.app.ui.screens.StartupAnimation
import com.brasa.app.ui.screens.PageIndicator
import com.brasa.app.ui.screens.Home
import com.brasa.app.ui.screens.Breathe
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.Tokens

private const val MAIN = "principal"
private const val BREATHE = "respirar"
private const val PAGES = 6

/**
 * Navegación: arranque → páginas principales (deslizar horizontal: Inicio · Ajustes; la fase 4 añade
 * Gráficas y Evolución) → Respirar como pantalla apilada (deslizar a la derecha para volver).
 */
@Composable
fun BrasaApp(vm: BrasaViewModel) {
  val settings by vm.settings.collectAsStateWithLifecycle()
  val state by vm.state.collectAsStateWithLifecycle()
  val askPermissions by vm.askPermissions.collectAsStateWithLifecycle()
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    vm.permissionsAnswered()
  }
  val requestPermissions = { launcher.launch(healthPermissions) }
  var started by rememberSaveable { mutableStateOf(false) }

  // Meta cumplida o subida de nivel: vibración de celebración; el nivel además tiene su animación.
  var levelUpEvent by remember { mutableStateOf<Celebration.Level?>(null) }
  val haptics = rememberHaptics()
  LaunchedEffect(Unit) {
    vm.celebrations.collect { c ->
      haptics.celebrate()
      if (c is Celebration.Level) levelUpEvent = c
    }
  }

  BrasaTheme(dark = settings.dark) {
   Box(Modifier.fillMaxSize()) {
    Crossfade(started, animationSpec = tween(Tokens.Duration.medium), label = "arranque") { ready ->
      if (!ready) {
        StartupAnimation(onFinish = { started = true })
      } else if (askPermissions) {
        Permissions(onAllow = requestPermissions, onNotNow = vm::permissionsAnswered)
      } else {
        val nav = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(nav, startDestination = MAIN) {
          composable(MAIN) {
            MainPages(
              state, settings,
              SettingsActions(
                theme = { vm.theme(it) }, demo = { vm.demo(it) },
                emotion = { vm.demoEmotion(it) }, stage = { vm.demoStage(it) }, permissions = requestPermissions,
              ),
              onTapBrasa = vm::tapBrasa, onBreathe = { nav.navigate(BREATHE) },
            )
          }
          composable(BREATHE) {
            // Se fija al entrar: el "antes" no cambia durante el ejercicio.
            val before = androidx.compose.runtime.remember { vm.heartRateBeforeBreathing() }
            Breathe(
              stage = state.stage,
              heartRateBefore = before,
              heartRateAfter = vm::heartRateAfterBreathing,
              onCompleted = vm::breathCompleted,
              onReady = { nav.popBackStack() },
            )
          }
        }
      }
    }
    levelUpEvent?.let { LevelUp(it.level, it.stage, it.newStage, onFinish = { levelUpEvent = null }) }
   }
  }
}

/**
 * Páginas principales (deslizar en horizontal o girar la corona): Inicio · Pasos · Pulso · Semana ·
 * Evolución · Ajustes. En Ajustes la corona desplaza la lista en lugar de cambiar de página.
 */
@Composable
fun MainPages(
  state: BrasaState,
  settings: AppSettings,
  actions: SettingsActions,
  onTapBrasa: () -> Unit,
  onBreathe: () -> Unit,
  initialPage: Int = 0,
) {
  val pages = rememberPagerState(initialPage = initialPage) { PAGES }
  val scope = rememberCoroutineScope()
  val focus = remember { FocusRequester() }
  var accumulated by remember { mutableFloatStateOf(0f) }
  Box(
    Modifier
      .fillMaxSize()
      // Corona / bisel giratorio: pasa de página entre Inicio, gráficas y evolución.
      .onRotaryScrollEvent { e ->
        if (pages.currentPage == PAGES - 1) return@onRotaryScrollEvent false
        accumulated += e.verticalScrollPixels
        if (kotlin.math.abs(accumulated) > 60f) {
          val target = (pages.currentPage + if (accumulated > 0) 1 else -1).coerceIn(0, PAGES - 1)
          accumulated = 0f
          scope.launch { pages.animateScrollToPage(target) }
        }
        true
      }
      .focusRequester(focus)
      .focusable(),
  ) {
    LaunchedEffect(Unit) { focus.requestFocus() }
    HorizontalPager(pages, Modifier.fillMaxSize(), beyondViewportPageCount = 0) { i ->
      when (i) {
        0 -> Home(state, onTapBrasa = onTapBrasa, onBreathe = onBreathe)
        1 -> StepsChart(state)
        2 -> HeartRateChart(state)
        3 -> WeekChart(state, goalPhrase = state.goalPhrase)
        4 -> Evolution(state)
        else -> SettingsScreen(settings, actions, source = state.source)
      }
    }
    PageIndicator(total = PAGES, current = pages.currentPage)
  }
}
