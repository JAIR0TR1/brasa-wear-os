package com.brasa.app.ui.pantallas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.brasa.app.EstadoBrasa
import com.brasa.app.Fuente
import com.brasa.app.R
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.brasa.rememberAnimacionesActivas
import com.brasa.app.ui.componentes.AnilloBorde
import com.brasa.app.ui.componentes.BotonBrasa
import com.brasa.app.ui.componentes.IconoRespirar
import com.brasa.app.ui.componentes.TituloBorde
import com.brasa.app.ui.rememberHaptica
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Frases locales por emoción (respaldo sin red; la fase 5 añade la IA). */
fun frasesDe(e: Emocion, saludando: Boolean) = when {
  saludando -> R.array.frases_saludo
  e == Emocion.APAGADA -> R.array.frases_apagada
  e == Emocion.INQUIETA -> R.array.frases_inquieta
  e == Emocion.TRANQUILA -> R.array.frases_tranquila
  e == Emocion.FELIZ -> R.array.frases_feliz
  else -> R.array.frases_euforica
}

/**
 * Inicio: Brasa grande con su emoción y etapa, anillo de meta abrazando el borde, hora curva,
 * pasos de hoy y la frase de Brasa. Tocar a Brasa: rebote, risa y vibración.
 */
@Composable
fun Inicio(
  estado: EstadoBrasa,
  onTocarBrasa: () -> Unit,
  onRespirar: () -> Unit,
  entrada: Boolean = true,
  tiempoFijo: Float? = null,
  hora: String? = null,
) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(estado.emocion, colores.oscuro).acento
  val haptica = rememberHaptica()
  val animado = rememberAnimacionesActivas() && tiempoFijo == null
  var toques by remember { mutableIntStateOf(0) }

  // Brasa nace con rebote y vibración corta (continuación del arranque).
  val nacer = remember { Animatable(if (entrada && animado) 0.2f else 1f) }
  LaunchedEffect(Unit) {
    if (entrada && animado) {
      haptica.nacer()
      nacer.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f))
    }
  }

  // Hora (se actualiza cada 15 s; texto curvo arriba).
  var reloj by remember { mutableStateOf(hora ?: horaActual()) }
  if (hora == null) LaunchedEffect(Unit) { while (true) { delay(15_000); reloj = horaActual() } }

  val locale = LocalConfiguration.current.locales[0]
  val pasos = remember(estado.pasos, locale) { NumberFormat.getIntegerInstance(locale).format(estado.pasos) }
  val meta = remember(estado.meta, locale) { NumberFormat.getIntegerInstance(locale).format(estado.meta) }
  val frases = stringArrayResource(frasesDe(estado.emocion, estado.saludando))
  // Modo manual (sin sensores o sin permiso): la primera frase enseña a alimentar a Brasa.
  val frase = if (estado.fuente == Fuente.MANUAL && estado.pasos == 0) stringResource(R.string.manual_pista)
    else estado.fraseIa ?: frases[estado.frase.mod(frases.size)]
  val descAnillo = stringResource(R.string.anillo_descripcion, (estado.progreso * 100).toInt())

  FondoEmocion(estado.emocion) {
    AnilloBorde(estado.progreso, acento, animado = animado, descripcion = descAnillo)
    TituloBorde(reloj, margen = 20.dp)
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Espacio.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Hueco fijo para la hora curva: Brasa nunca la pisa.
      Spacer(Modifier.height(36.dp))
      Brasa(
        emocion = estado.emocion,
        etapa = estado.etapa,
        reaccion = toques,
        tiempoFijo = tiempoFijo,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          // 1.14 creciendo hacia abajo: Brasa es la protagonista (puede invadir un poco su margen interno, nunca el texto).
          .graphicsLayer { scaleX = nacer.value * 1.14f; scaleY = nacer.value * 1.14f; alpha = nacer.value.coerceIn(0f, 1f); transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.15f) }
          .clickable(remember { MutableInteractionSource() }, indication = null) {
            toques++
            haptica.toque()
            onTocarBrasa()
          },
      )
      // Pasos de hoy: cifra protagonista + meta en mono.
      Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.semantics(mergeDescendants = true) {},
      ) {
        Text(pasos, style = Tipo.cifra.copy(fontSize = 25.sp), color = colores.texto, maxLines = 1)
        Spacer(Modifier.width(Tokens.Espacio.xs))
        Text("/ $meta", style = Tipo.dato, color = colores.textoSecundario, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
      }
      // Frase de Brasa (o botón Respirar si está inquieta).
      Box(Modifier.heightIn(min = 36.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
          targetState = if (estado.emocion == Emocion.INQUIETA) null else frase,
          transitionSpec = { fadeIn(tween(Tokens.Duracion.media)) togetherWith fadeOut(tween(Tokens.Duracion.rapida)) },
          label = "frase",
        ) { f ->
          if (f == null) {
            BotonBrasa(
              stringResource(R.string.respirar), acento, onRespirar,
              icono = { IconoRespirar(if (colores.oscuro) Tokens.Colores.Negro else androidx.compose.ui.graphics.Color.White) },
            )
          } else {
            // Abajo el círculo se estrecha: la frase queda dentro del anillo y, si no cabe en 2 líneas,
            // reduce su tamaño (hasta 11 sp) en lugar de cortarse.
            androidx.compose.foundation.text.BasicText(
              f, style = Tipo.voz.copy(color = acento), maxLines = 2, overflow = TextOverflow.Ellipsis,
              autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = Tipo.voz.fontSize),
              modifier = Modifier.padding(horizontal = Tokens.Espacio.xl),
            )
          }
        }
      }
      Spacer(Modifier.height(Tokens.Espacio.l + Tokens.Espacio.xs))
    }
  }
}

private fun horaActual(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
