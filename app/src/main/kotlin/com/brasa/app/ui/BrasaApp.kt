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
import com.brasa.app.ui.pantallas.Evolucion
import com.brasa.app.ui.pantallas.GraficaPasos
import com.brasa.app.ui.pantallas.GraficaPulso
import com.brasa.app.ui.pantallas.GraficaSemana
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.brasa.app.BrasaViewModel
import com.brasa.app.data.permisosSalud
import com.brasa.app.ui.pantallas.Permisos
import com.brasa.app.ui.pantallas.AccionesAjustes
import com.brasa.app.ui.pantallas.AjustesPantalla
import com.brasa.app.ui.pantallas.Arranque
import com.brasa.app.ui.pantallas.IndicadorPaginas
import com.brasa.app.ui.pantallas.Inicio
import com.brasa.app.ui.pantallas.Respirar
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.Tokens

private const val PRINCIPAL = "principal"
private const val RESPIRAR = "respirar"
private const val PAGINAS = 6

/**
 * Navegación: arranque → páginas principales (deslizar horizontal: Inicio · Ajustes; la fase 4 añade
 * Gráficas y Evolución) → Respirar como pantalla apilada (deslizar a la derecha para volver).
 */
@Composable
fun BrasaApp(vm: BrasaViewModel) {
  val ajustes by vm.ajustes.collectAsStateWithLifecycle()
  val estado by vm.estado.collectAsStateWithLifecycle()
  val pedirPermisos by vm.pedirPermisos.collectAsStateWithLifecycle()
  val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
    vm.permisosRespondidos()
  }
  val pedir = { lanzador.launch(permisosSalud) }
  var arrancado by rememberSaveable { mutableStateOf(false) }

  BrasaTheme(oscuro = ajustes.oscuro) {
    Crossfade(arrancado, animationSpec = tween(Tokens.Duracion.media), label = "arranque") { listo ->
      if (!listo) {
        Arranque(onFin = { arrancado = true })
      } else if (pedirPermisos) {
        Permisos(onPermitir = pedir, onAhoraNo = vm::permisosRespondidos)
      } else {
        val nav = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(nav, startDestination = PRINCIPAL) {
          composable(PRINCIPAL) {
            val paginas = rememberPagerState { PAGINAS }
            val alcance = rememberCoroutineScope()
            val foco = remember { FocusRequester() }
            var acumulado by remember { mutableFloatStateOf(0f) }
            Box(
              Modifier
                .fillMaxSize()
                // Corona / bisel giratorio: pasa de página entre Inicio, gráficas y evolución.
                .onRotaryScrollEvent { e ->
                  if (paginas.currentPage == PAGINAS - 1) return@onRotaryScrollEvent false
                  acumulado += e.verticalScrollPixels
                  if (kotlin.math.abs(acumulado) > 60f) {
                    val destino = (paginas.currentPage + if (acumulado > 0) 1 else -1).coerceIn(0, PAGINAS - 1)
                    acumulado = 0f
                    alcance.launch { paginas.animateScrollToPage(destino) }
                  }
                  true
                }
                .focusRequester(foco)
                .focusable(),
            ) {
              LaunchedEffect(Unit) { foco.requestFocus() }
              HorizontalPager(paginas, Modifier.fillMaxSize(), beyondViewportPageCount = 0) { i ->
                when (i) {
                  0 -> Inicio(estado, onTocarBrasa = vm::tocarBrasa, onRespirar = { nav.navigate(RESPIRAR) })
                  1 -> GraficaPasos(estado)
                  2 -> GraficaPulso(estado)
                  3 -> GraficaSemana(estado)
                  4 -> Evolucion(estado)
                  else -> AjustesPantalla(
                    ajustes,
                    AccionesAjustes(
                      tema = { vm.tema(it) }, demo = { vm.demo(it) },
                      emocion = { vm.emocionDemo(it) }, etapa = { vm.etapaDemo(it) }, permisos = pedir,
                    ),
                    fuente = estado.fuente,
                  )
                }
              }
              IndicadorPaginas(total = PAGINAS, actual = paginas.currentPage)
            }
          }
          composable(RESPIRAR) {
            // Se fija al entrar: el "antes" no cambia durante el ejercicio.
            val antes = androidx.compose.runtime.remember { vm.pulsoAntesRespirar() }
            Respirar(
              etapa = estado.etapa,
              pulsoAntes = antes,
              pulsoDespues = vm::pulsoDespuesRespirar,
              onCompletado = vm::respiroCompletado,
              onListo = { nav.popBackStack() },
            )
          }
        }
      }
    }
  }
}
