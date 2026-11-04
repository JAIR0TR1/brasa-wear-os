package com.brasa.app.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.R
import com.brasa.app.ui.componentes.TituloBorde
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.brasa.nombreEmocion
import com.brasa.app.ui.brasa.nombreEtapa
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens
import kotlin.math.cos
import kotlin.math.sin

/** Una página de la galería: qué emoción y etapa mostrar, y qué regla la activa. */
data class PaginaGaleria(val emocion: Emocion, val etapa: Etapa, val esEtapa: Boolean, val detalle: Int)

val paginasGaleria = listOf(
  PaginaGaleria(Emocion.APAGADA, Etapa.LLAMA, false, R.string.regla_apagada),
  PaginaGaleria(Emocion.INQUIETA, Etapa.LLAMA, false, R.string.regla_inquieta),
  PaginaGaleria(Emocion.TRANQUILA, Etapa.LLAMA, false, R.string.regla_tranquila),
  PaginaGaleria(Emocion.FELIZ, Etapa.LLAMA, false, R.string.regla_feliz),
  PaginaGaleria(Emocion.EUFORICA, Etapa.LLAMA, false, R.string.regla_euforica),
  PaginaGaleria(Emocion.FELIZ, Etapa.CHISPA, true, R.string.niveles_chispa),
  PaginaGaleria(Emocion.FELIZ, Etapa.LLAMA, true, R.string.niveles_llama),
  PaginaGaleria(Emocion.FELIZ, Etapa.FOGATA, true, R.string.niveles_fogata),
  PaginaGaleria(Emocion.FELIZ, Etapa.LEYENDA, true, R.string.niveles_leyenda),
)

/** Pantalla de prueba de la fase 1: desliza para ver cada emoción y etapa; toca a Brasa para que reaccione. */
@Composable
fun GaleriaPantalla() {
  val estado = rememberPagerState { paginasGaleria.size }
  Box(Modifier.fillMaxSize()) {
    HorizontalPager(estado, Modifier.fillMaxSize()) { i ->
      ContenidoGaleria(paginasGaleria[i])
    }
    IndicadorPaginas(total = paginasGaleria.size, actual = estado.currentPage)
  }
}

@Composable
fun ContenidoGaleria(pagina: PaginaGaleria, tiempoFijo: Float? = null) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(pagina.emocion, colores.oscuro).acento
  val haptica = LocalHapticFeedback.current
  var toques by remember { mutableIntStateOf(0) }

  FondoEmocion(pagina.emocion) {
    TituloBorde(stringResource(if (pagina.esEtapa) R.string.galeria_evolucion else R.string.galeria_emociones))
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Espacio.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.weight(0.24f))
      Brasa(
        emocion = pagina.emocion,
        etapa = pagina.etapa,
        reaccion = toques,
        tiempoFijo = tiempoFijo,
        modifier = Modifier
          .fillMaxWidth(0.86f)
          .weight(1f)
          .clickable(remember { MutableInteractionSource() }, indication = null) {
            toques++
            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
          },
      )
      Text(
        stringResource(if (pagina.esEtapa) nombreEtapa(pagina.etapa) else nombreEmocion(pagina.emocion)),
        style = Tipo.titulo, color = colores.texto, maxLines = 1, overflow = TextOverflow.Ellipsis,
      )
      Text(
        stringResource(pagina.detalle),
        style = Tipo.dato, color = acento, maxLines = 2, overflow = TextOverflow.Ellipsis,
        // Más margen abajo: el círculo se estrecha y el texto debe quedar dentro.
        modifier = Modifier.padding(horizontal = Tokens.Espacio.l),
      )
      Spacer(Modifier.height(Tokens.Espacio.xl))
    }
  }
}

/** Indicador de página en arco, abrazando el borde inferior. */
@Composable
fun IndicadorPaginas(total: Int, actual: Int) {
  val colores = BrasaTema.colores
  Canvas(Modifier.fillMaxSize()) {
    val radio = size.minDimension / 2f - 7.dp.toPx()
    val paso = 5.5f
    val inicio = 90f + paso * (total - 1) / 2f
    for (i in 0 until total) {
      val ang = Math.toRadians((inicio - i * paso).toDouble())
      val c = Offset(center.x + (cos(ang) * radio).toFloat(), center.y + (sin(ang) * radio).toFloat())
      val activo = i == actual
      drawCircle(
        if (activo) colores.fuego else colores.textoSecundario.copy(alpha = 0.4f),
        radius = (if (activo) 3.2f else 2.2f).dp.toPx(),
        center = c,
      )
    }
  }
}
