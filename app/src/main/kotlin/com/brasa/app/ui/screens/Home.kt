package com.brasa.app.ui.screens

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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.brasa.app.BrasaState
import com.brasa.app.DataSource
import com.brasa.app.R
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.brasa.rememberAnimationsEnabled
import com.brasa.app.ui.components.EdgeRing
import com.brasa.app.ui.components.BrasaButton
import com.brasa.app.ui.components.BreatheIcon
import com.brasa.app.ui.components.EdgeTitle
import com.brasa.app.ui.rememberHaptics
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Frases locales por emoción (respaldo sin red; la fase 5 añade la IA). */
fun phrasesFor(e: Emotion, greeting: Boolean) = when {
  greeting -> R.array.phrases_greeting
  e == Emotion.DIM -> R.array.phrases_dim
  e == Emotion.RESTLESS -> R.array.phrases_restless
  e == Emotion.CALM -> R.array.phrases_calm
  e == Emotion.HAPPY -> R.array.phrases_happy
  else -> R.array.phrases_ecstatic
}

/**
 * Inicio: Brasa grande con su emoción y etapa, anillo de meta abrazando el borde, hora curva,
 * pasos de hoy y la frase de Brasa. Tocar a Brasa: rebote, risa y vibración.
 */
@Composable
fun Home(
  state: BrasaState,
  onTapBrasa: () -> Unit,
  onBreathe: () -> Unit,
  entrance: Boolean = true,
  fixedTime: Float? = null,
  timeText: String? = null,
) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(state.emotion, colors.dark).accent
  val haptics = rememberHaptics()
  val animated = rememberAnimationsEnabled() && fixedTime == null
  var taps by remember { mutableIntStateOf(0) }

  // Brasa nace con rebote y vibración corta (continuación del arranque).
  val birth = remember { Animatable(if (entrance && animated) 0.2f else 1f) }
  LaunchedEffect(Unit) {
    if (entrance && animated) {
      haptics.birth()
      birth.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 260f))
    }
  }

  // Hora (se actualiza cada 15 s; texto curvo arriba).
  var clock by remember { mutableStateOf(timeText ?: currentHour()) }
  if (timeText == null) LaunchedEffect(Unit) { while (true) { delay(15_000); clock = currentHour() } }

  val locale = LocalConfiguration.current.locales[0]
  val steps = remember(state.steps, locale) { NumberFormat.getIntegerInstance(locale).format(state.steps) }
  val goal = remember(state.goal, locale) { NumberFormat.getIntegerInstance(locale).format(state.goal) }
  val phrases = stringArrayResource(phrasesFor(state.emotion, state.greeting))
  // Modo manual (sin sensores o sin permiso): la primera frase enseña a alimentar a Brasa.
  val phrase = if (state.source == DataSource.MANUAL && state.steps == 0) stringResource(R.string.manual_hint)
    else state.aiPhrase ?: phrases[state.phrase.mod(phrases.size)]
  val ringDescription = stringResource(R.string.ring_description, (state.progress * 100).toInt())

  val stepsDescription = stringResource(R.string.steps_of_goal, steps, goal)
  val brasaAction = stringResource(if (state.source == DataSource.MANUAL) R.string.action_give_firewood else R.string.action_tap_brasa)
  // Orden de lectura: hora → Brasa → pasos → frase o botón → anillo de la meta.
  EmotionBackground(state.emotion, Modifier.semantics { isTraversalGroup = true }) {
    EdgeRing(state.progress, accent, Modifier.semantics { traversalIndex = 1f }, animated = animated, description = ringDescription)
    EdgeTitle(clock, margin = 20.dp, isHeading = false)
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Spacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Hueco fijo para la hora curva: Brasa nunca la pisa.
      Spacer(Modifier.height(36.dp))
      Brasa(
        emotion = state.emotion,
        stage = state.stage,
        reaction = taps,
        fixedTime = fixedTime,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          // 1.14 creciendo hacia abajo: Brasa es la protagonista (puede invadir un poco su margen interno, nunca el texto).
          .graphicsLayer { scaleX = birth.value * 1.14f; scaleY = birth.value * 1.14f; alpha = birth.value.coerceIn(0f, 1f); transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.15f) }
          .clickable(remember { MutableInteractionSource() }, indication = null, onClickLabel = brasaAction) {
            taps++
            haptics.tap()
            onTapBrasa()
          },
      )
      // Pasos de hoy: cifra protagonista + meta en mono.
      Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.clearAndSetSemantics { contentDescription = stepsDescription },
      ) {
        Text(steps, style = BrasaType.figure.copy(fontSize = 25.sp), color = colors.text, maxLines = 1)
        Spacer(Modifier.width(Tokens.Spacing.xs))
        Text("/ $goal", style = BrasaType.data, color = colors.secondaryText, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
      }
      // Frase de Brasa (o botón Respirar si está inquieta).
      Box(Modifier.heightIn(min = 36.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
          targetState = if (state.emotion == Emotion.RESTLESS) null else phrase,
          transitionSpec = { fadeIn(tween(Tokens.Duration.medium)) togetherWith fadeOut(tween(Tokens.Duration.fast)) },
          label = "frase",
        ) { f ->
          if (f == null) {
            BrasaButton(
              stringResource(R.string.breathe), accent, onBreathe,
              icon = { BreatheIcon(if (colors.dark) Tokens.Colors.Black else androidx.compose.ui.graphics.Color.White) },
            )
          } else {
            // Abajo el círculo se estrecha: la frase queda dentro del anillo y, si no cabe en 2 líneas,
            // reduce su tamaño (hasta 11 sp) en lugar de cortarse.
            androidx.compose.foundation.text.BasicText(
              f, style = BrasaType.voice.copy(color = accent), maxLines = 2, overflow = TextOverflow.Ellipsis,
              autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = BrasaType.voice.fontSize),
              modifier = Modifier.padding(horizontal = Tokens.Spacing.xl),
            )
          }
        }
      }
      Spacer(Modifier.height(Tokens.Spacing.l + Tokens.Spacing.xs))
    }
  }
}

private fun currentHour(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
