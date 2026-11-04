package com.brasa.app.ui.pantallas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.R
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.componentes.AnilloBorde
import com.brasa.app.ui.componentes.BotonBrasa
import com.brasa.app.ui.componentes.TituloBorde
import com.brasa.app.ui.rememberHaptica
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class FaseRespiro { PREPARA, INHALA, EXHALA, FIN }

/** Ritmo: 6 ciclos × (4 s inhalar + 6 s exhalar) = 60 s. Exhalar más largo ayuda a calmarse. */
const val INHALA_MS = 4000
const val EXHALA_MS = 6000
const val CICLOS = 6
const val TOTAL_S = CICLOS * (INHALA_MS + EXHALA_MS) / 1000

private const val ESCALA_MIN = 0.74f
private const val ESCALA_MAX = 1.14f

/**
 * Respiración guiada de 1 minuto: Brasa se expande al inhalar y se contrae al exhalar,
 * con vibraciones de ritmo; al final muestra el pulso antes/después.
 */
@Composable
fun Respirar(
  etapa: Etapa,
  pulsoAntes: Int?,
  pulsoDespues: (Int) -> Int,
  onCompletado: (antes: Int?, despues: Int?) -> Unit,
  onListo: () -> Unit,
  faseFija: FaseRespiro? = null,
  tiempoFijo: Float? = null,
) {
  val haptica = rememberHaptica()
  val escala = remember { Animatable(if (faseFija == FaseRespiro.INHALA) 1.02f else ESCALA_MIN) }
  val total = remember { Animatable(if (faseFija != null) 0.38f else 0f) }
  var fase by remember { mutableStateOf(faseFija ?: FaseRespiro.PREPARA) }
  var restante by remember { mutableIntStateOf(if (faseFija != null) 37 else TOTAL_S) }
  var despues by remember { mutableStateOf(if (faseFija != null) pulsoAntes?.let(pulsoDespues) else null) }

  if (faseFija == null) {
    LaunchedEffect(Unit) {
      delay(1500)
      launch { total.animateTo(1f, tween(TOTAL_S * 1000, easing = LinearEasing)) }
      launch { while (restante > 0) { delay(1000); restante-- } }
      repeat(CICLOS) {
        fase = FaseRespiro.INHALA
        haptica.inhala()
        escala.animateTo(ESCALA_MAX, tween(INHALA_MS, easing = FastOutSlowInEasing))
        fase = FaseRespiro.EXHALA
        haptica.exhala()
        escala.animateTo(ESCALA_MIN, tween(EXHALA_MS, easing = FastOutSlowInEasing))
      }
      despues = pulsoAntes?.let(pulsoDespues)
      fase = FaseRespiro.FIN
      haptica.celebrar()
      onCompletado(pulsoAntes, despues)
    }
  }

  if (fase == FaseRespiro.FIN) {
    ResultadoRespiro(etapa, pulsoAntes, despues, onListo, tiempoFijo)
    return
  }

  val colores = BrasaTema.colores
  val acento = Tokens.paleta(Emocion.TRANQUILA, colores.oscuro).acento
  // El fondo "respira" con Brasa: más intenso al inhalar.
  val intensidad = 0.55f + 0.45f * ((escala.value - ESCALA_MIN) / (ESCALA_MAX - ESCALA_MIN))

  FondoEmocion(Emocion.TRANQUILA, intensidad = intensidad) {
    AnilloBorde(total.value, acento, animado = false)
    TituloBorde(stringResource(R.string.respirar), margen = 16.dp)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(Modifier.weight(0.22f))
      Box(Modifier.fillMaxWidth(0.8f).weight(1f), contentAlignment = Alignment.Center) {
        // Ondas de aire que crecen con la respiración.
        Canvas(Modifier.fillMaxSize()) {
          val e = escala.value
          val base = size.minDimension * 0.36f
          for (i in 0..2) {
            drawCircle(
              acento.copy(alpha = (0.32f - i * 0.09f) * intensidad),
              radius = base * e * (1f + i * 0.16f),
              style = Stroke(width = (2.2f - i * 0.5f).dp.toPx()),
            )
          }
        }
        Brasa(
          emocion = Emocion.TRANQUILA, etapa = etapa, tiempoFijo = tiempoFijo,
          modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = escala.value; scaleY = escala.value },
        )
      }
      AnimatedContent(
        fase,
        transitionSpec = { fadeIn(tween(Tokens.Duracion.media)) togetherWith fadeOut(tween(Tokens.Duracion.media)) },
        label = "fase",
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      ) { f ->
        Text(
          stringResource(
            when (f) {
              FaseRespiro.PREPARA -> R.string.respiro_prepara
              FaseRespiro.INHALA -> R.string.respiro_inhala
              else -> R.string.respiro_exhala
            },
          ),
          style = Tipo.titulo, color = colores.texto, maxLines = 1,
        )
      }
      Text(
        "%d:%02d".format(restante / 60, restante % 60),
        style = Tipo.dato, color = colores.textoSecundario,
      )
      Spacer(Modifier.height(Tokens.Espacio.xl + Tokens.Espacio.xs))
    }
  }
}

@Composable
private fun ResultadoRespiro(etapa: Etapa, antes: Int?, despues: Int?, onListo: () -> Unit, tiempoFijo: Float?) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(Emocion.FELIZ, colores.oscuro).acento
  FondoEmocion(Emocion.FELIZ) {
    TituloBorde(stringResource(R.string.respiro_completo), margen = Tokens.Espacio.s)
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Espacio.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.weight(0.25f))
      Brasa(Emocion.FELIZ, etapa, Modifier.fillMaxWidth(0.62f).weight(1f), tiempoFijo = tiempoFijo)
      if (antes != null && despues != null && despues < antes) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text((despues - antes).let { if (it < 0) "−${-it}" else "+$it" }, style = Tipo.cifra, color = colores.texto)
          Spacer(Modifier.width(Tokens.Espacio.xs))
          Text(stringResource(R.string.lpm), style = Tipo.dato, color = colores.textoSecundario, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text(stringResource(R.string.respiro_antes_despues, antes, despues), style = Tipo.dato, color = acento)
      } else {
        // Si el pulso no bajó (o no hay sensor) no se presenta como logro: Brasa valora el minuto igualmente.
        Text(stringResource(R.string.respiro_sin_pulso), style = Tipo.voz, color = acento, maxLines = 2)
        if (antes != null && despues != null) {
          Text(stringResource(R.string.respiro_antes_despues, antes, despues), style = Tipo.dato, color = colores.textoSecundario)
        }
      }
      Spacer(Modifier.height(Tokens.Espacio.s))
      BotonBrasa(stringResource(R.string.listo), acento, onListo)
      Spacer(Modifier.height(Tokens.Espacio.l))
    }
  }
}
