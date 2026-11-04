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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.brasa.app.BrasaViewModel
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

/**
 * Navegación: arranque → páginas principales (deslizar horizontal: Inicio · Ajustes; la fase 4 añade
 * Gráficas y Evolución) → Respirar como pantalla apilada (deslizar a la derecha para volver).
 */
@Composable
fun BrasaApp(vm: BrasaViewModel) {
  val ajustes by vm.ajustes.collectAsStateWithLifecycle()
  val estado by vm.estado.collectAsStateWithLifecycle()
  var arrancado by rememberSaveable { mutableStateOf(false) }

  BrasaTheme(oscuro = ajustes.oscuro) {
    Crossfade(arrancado, animationSpec = tween(Tokens.Duracion.media), label = "arranque") { listo ->
      if (!listo) {
        Arranque(onFin = { arrancado = true })
      } else {
        val nav = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(nav, startDestination = PRINCIPAL) {
          composable(PRINCIPAL) {
            val paginas = rememberPagerState { 2 }
            Box(Modifier.fillMaxSize()) {
              HorizontalPager(paginas, Modifier.fillMaxSize()) { i ->
                when (i) {
                  0 -> Inicio(estado, onTocarBrasa = vm::tocarBrasa, onRespirar = { nav.navigate(RESPIRAR) })
                  else -> AjustesPantalla(
                    ajustes,
                    AccionesAjustes(
                      tema = { vm.tema(it) }, demo = { vm.demo(it) },
                      emocion = { vm.emocionDemo(it) }, etapa = { vm.etapaDemo(it) },
                    ),
                  )
                }
              }
              IndicadorPaginas(total = 2, actual = paginas.currentPage)
            }
          }
          composable(RESPIRAR) {
            val antes = vm.pulsoAntesRespirar()
            Respirar(
              etapa = estado.etapa,
              pulsoAntes = antes,
              pulsoDespues = vm::pulsoDespuesRespirar,
              onCompletado = { vm.emocionDemo(null) },
              onListo = { nav.popBackStack() },
            )
          }
        }
      }
    }
  }
}
