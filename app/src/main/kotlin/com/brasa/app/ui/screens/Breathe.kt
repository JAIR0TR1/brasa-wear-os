package com.brasa.app.ui.screens

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
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.components.EdgeRing
import com.brasa.app.ui.components.BrasaButton
import com.brasa.app.ui.components.EdgeTitle
import com.brasa.app.ui.rememberHaptics
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class BreathPhase { PREPARE, INHALE, EXHALE, DONE }

/** Ritmo: 6 ciclos × (4 s inhalar + 6 s exhalar) = 60 s. Exhalar más largo ayuda a calmarse. */
const val INHALE_MS = 4000
const val EXHALE_MS = 6000
const val CYCLES = 6
const val TOTAL_SECONDS = CYCLES * (INHALE_MS + EXHALE_MS) / 1000

private const val SCALE_MIN = 0.74f
private const val SCALE_MAX = 1.14f

/**
 * Respiración guiada de 1 minuto: Brasa se expande al inhalar y se contrae al exhalar,
 * con vibraciones de ritmo; al final muestra el pulso antes/después.
 */
@Composable
fun Breathe(
  stage: Stage,
  heartRateBefore: Int?,
  heartRateAfter: (Int) -> Int,
  onCompleted: (before: Int?, after: Int?) -> Unit,
  onReady: () -> Unit,
  fixedPhase: BreathPhase? = null,
  fixedTime: Float? = null,
) {
  val haptics = rememberHaptics()
  val scale = remember { Animatable(if (fixedPhase == BreathPhase.INHALE) 1.02f else SCALE_MIN) }
  val total = remember { Animatable(if (fixedPhase != null) 0.38f else 0f) }
  var phase by remember { mutableStateOf(fixedPhase ?: BreathPhase.PREPARE) }
  var secondsLeft by remember { mutableIntStateOf(if (fixedPhase != null) 37 else TOTAL_SECONDS) }
  var after by remember { mutableStateOf(if (fixedPhase != null) heartRateBefore?.let(heartRateAfter) else null) }

  if (fixedPhase == null) {
    LaunchedEffect(Unit) {
      delay(1500)
      launch { total.animateTo(1f, tween(TOTAL_SECONDS * 1000, easing = LinearEasing)) }
      launch { while (secondsLeft > 0) { delay(1000); secondsLeft-- } }
      repeat(CYCLES) {
        phase = BreathPhase.INHALE
        haptics.inhale()
        scale.animateTo(SCALE_MAX, tween(INHALE_MS, easing = FastOutSlowInEasing))
        phase = BreathPhase.EXHALE
        haptics.exhale()
        scale.animateTo(SCALE_MIN, tween(EXHALE_MS, easing = FastOutSlowInEasing))
      }
      after = heartRateBefore?.let(heartRateAfter)
      phase = BreathPhase.DONE
      haptics.celebrate()
      onCompleted(heartRateBefore, after)
    }
  }

  if (phase == BreathPhase.DONE) {
    BreathResult(stage, heartRateBefore, after, onReady, fixedTime)
    return
  }

  val colors = BrasaTheme.colors
  val accent = Tokens.palette(Emotion.CALM, colors.dark).accent
  // El fondo "respira" con Brasa: más intenso al inhalar.
  val intensity = 0.55f + 0.45f * ((scale.value - SCALE_MIN) / (SCALE_MAX - SCALE_MIN))

  EmotionBackground(Emotion.CALM, intensity = intensity) {
    EdgeRing(total.value, accent, animated = false)
    EdgeTitle(stringResource(R.string.breathe), margin = 16.dp)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(Modifier.weight(0.22f))
      Box(Modifier.fillMaxWidth(0.8f).weight(1f), contentAlignment = Alignment.Center) {
        // Ondas de aire que crecen con la respiración.
        Canvas(Modifier.fillMaxSize()) {
          val e = scale.value
          val base = size.minDimension * 0.36f
          for (i in 0..2) {
            drawCircle(
              accent.copy(alpha = (0.32f - i * 0.09f) * intensity),
              radius = base * e * (1f + i * 0.16f),
              style = Stroke(width = (2.2f - i * 0.5f).dp.toPx()),
            )
          }
        }
        Brasa(
          emotion = Emotion.CALM, stage = stage, fixedTime = fixedTime,
          modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        )
      }
      AnimatedContent(
        phase,
        transitionSpec = { fadeIn(tween(Tokens.Duration.medium)) togetherWith fadeOut(tween(Tokens.Duration.medium)) },
        label = "fase",
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      ) { f ->
        Text(
          stringResource(
            when (f) {
              BreathPhase.PREPARE -> R.string.breath_prepare
              BreathPhase.INHALE -> R.string.breath_inhale
              else -> R.string.breath_exhale
            },
          ),
          style = BrasaType.title, color = colors.text, maxLines = 1,
        )
      }
      Text(
        "%d:%02d".format(secondsLeft / 60, secondsLeft % 60),
        style = BrasaType.data, color = colors.secondaryText,
      )
      Spacer(Modifier.height(Tokens.Spacing.xl + Tokens.Spacing.xs))
    }
  }
}

@Composable
private fun BreathResult(stage: Stage, before: Int?, after: Int?, onReady: () -> Unit, fixedTime: Float?) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(Emotion.HAPPY, colors.dark).accent
  EmotionBackground(Emotion.HAPPY) {
    EdgeTitle(stringResource(R.string.breath_complete), margin = Tokens.Spacing.s)
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Spacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.weight(0.25f))
      Brasa(Emotion.HAPPY, stage, Modifier.fillMaxWidth(0.62f).weight(1f), fixedTime = fixedTime)
      if (before != null && after != null && after < before) {
        Row(verticalAlignment = Alignment.Bottom) {
          Text((after - before).let { if (it < 0) "−${-it}" else "+$it" }, style = BrasaType.figure, color = colors.text)
          Spacer(Modifier.width(Tokens.Spacing.xs))
          Text(stringResource(R.string.unit_bpm), style = BrasaType.data, color = colors.secondaryText, modifier = Modifier.padding(bottom = 5.dp))
        }
        Text(stringResource(R.string.breath_before_after, before, after), style = BrasaType.data, color = accent)
      } else {
        // Si el pulso no bajó (o no hay sensor) no se presenta como logro: Brasa valora el minuto igualmente.
        Text(stringResource(R.string.breath_no_heart_rate), style = BrasaType.voice, color = accent, maxLines = 2)
        if (before != null && after != null) {
          Text(stringResource(R.string.breath_before_after, before, after), style = BrasaType.data, color = colors.secondaryText)
        }
      }
      Spacer(Modifier.height(Tokens.Spacing.s))
      BrasaButton(stringResource(R.string.done), accent, onReady)
      Spacer(Modifier.height(Tokens.Spacing.l))
    }
  }
}
