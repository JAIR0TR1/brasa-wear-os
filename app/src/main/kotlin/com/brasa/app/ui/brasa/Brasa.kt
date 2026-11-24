package com.brasa.app.ui.brasa

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.brasa.app.R
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** true si el sistema NO pide reducir animaciones (escala de animación > 0). */
@Composable
fun rememberAnimationsEnabled(): Boolean {
  val ctx = LocalContext.current
  return remember {
    Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
  }
}

/**
 * Brasa animada. El tiempo solo se lee en la fase de dibujo (no recompone) y se detiene
 * cuando la pantalla no está activa (ciclo de vida < RESUMED) o si se piden animaciones reducidas.
 *
 * @param reaccion contador: cada incremento dispara rebote + risa (toque).
 * @param tiempoFijo congela la animación en ese instante (capturas y previews).
 */
@Composable
fun Brasa(
  emotion: Emotion,
  stage: Stage,
  modifier: Modifier = Modifier,
  reaction: Int = 0,
  animated: Boolean = rememberAnimationsEnabled(),
  fixedTime: Float? = null,
) {
  val dark = BrasaTheme.colors.dark
  val target = remember(emotion) { poseFor(emotion) }
  val targetShape = remember(stage) { shapeFor(stage) }

  var fromPose by remember { mutableStateOf(target) }
  var hacia by remember { mutableStateOf(target) }
  val progress = remember { Animatable(1f) }
  var fromShape by remember { mutableStateOf(targetShape) }
  var haciaForma by remember { mutableStateOf(targetShape) }
  val shapeProgress = remember { Animatable(1f) }
  val bounce = remember { Animatable(1f) }
  val laugh = remember { Animatable(0f) }
  val blink = remember { Animatable(0f) }

  // Transición entre emociones: rebote (sale y entra) + interpolación de toda la pose.
  LaunchedEffect(emotion) {
    if (hacia == target) return@LaunchedEffect
    fromPose = lerpPose(fromPose, hacia, progress.value)
    hacia = target
    progress.snapTo(0f)
    if (!animated) { progress.snapTo(1f); return@LaunchedEffect }
    launch {
      bounce.animateTo(0.84f, tween(Tokens.Duration.fast))
      bounce.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
    }
    progress.animateTo(1f, tween(Tokens.Duration.medium + 200, easing = FastOutSlowInEasing))
  }

  LaunchedEffect(stage) {
    if (haciaForma == targetShape) return@LaunchedEffect
    fromShape = lerpForma(fromShape, haciaForma, shapeProgress.value)
    haciaForma = targetShape
    shapeProgress.snapTo(0f)
    if (!animated) { shapeProgress.snapTo(1f); return@LaunchedEffect }
    launch {
      bounce.animateTo(1.15f, tween(Tokens.Duration.medium))
      bounce.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
    }
    shapeProgress.animateTo(1f, tween(Tokens.Duration.draw, easing = FastOutSlowInEasing))
  }

  // Reacción al toque: rebote y risa breve.
  LaunchedEffect(reaction) {
    if (reaction == 0) return@LaunchedEffect
    launch {
      bounce.snapTo(0.9f)
      bounce.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f))
    }
    laugh.animateTo(1f, tween(Tokens.Duration.fast))
    delay(700)
    laugh.animateTo(0f, tween(Tokens.Duration.medium))
  }

  val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val active = animated && fixedTime == null && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

  // Parpadeo aleatorio cada 3–6 s.
  LaunchedEffect(active) {
    while (active) {
      delay(Random.nextLong(3000, 6000))
      blink.animateTo(1f, tween(70))
      blink.animateTo(0f, tween(110))
    }
  }

  val time = remember { mutableFloatStateOf(fixedTime ?: 0f) }
  LaunchedEffect(active) {
    if (!active) return@LaunchedEffect
    val base = time.floatValue
    val start = withFrameNanos { it }
    while (true) {
      withFrameNanos { time.floatValue = base + (it - start) / 1_000_000_000f }
    }
  }

  val description = stringResource(
    R.string.brasa_description,
    stringResource(emotionName(emotion)),
    stringResource(stageName(stage)),
  )
  Canvas(modifier.semantics { contentDescription = description }) {
    drawBrasa(
      pose = lerpPose(fromPose, hacia, progress.value),
      shape = lerpForma(fromShape, haciaForma, shapeProgress.value),
      t = time.floatValue,
      blink = blink.value,
      bounce = bounce.value,
      laugh = laugh.value,
      dark = dark,
    )
  }
}

/**
 * Fondo de cada emoción: degradado radial del color de la emoción que se desvanece
 * a negro puro en los bordes (OLED: los píxeles negros no consumen).
 */
@Composable
fun EmotionBackground(
  emotion: Emotion,
  modifier: Modifier = Modifier,
  intensity: Float = 1f,
  content: @Composable BoxScope.() -> Unit,
) {
  val colors = BrasaTheme.colors
  val target = Tokens.palette(emotion, colors.dark).base
  val color by animateColorAsState(target, tween(Tokens.Duration.backgroundTransition), label = "fondo")
  Box(
    modifier
      .fillMaxSize()
      .drawBehind {
        drawRect(colors.background)
        val a = (if (colors.dark) 0.42f else 0.2f) * intensity
        drawRect(
          Brush.radialGradient(
            0f to color.copy(alpha = a),
            0.45f to color.copy(alpha = a * 0.38f),
            1f to Color.Transparent,
            center = center,
            radius = size.maxDimension * 0.62f,
          ),
        )
      },
    content = content,
  )
}

fun emotionName(e: Emotion) = when (e) {
  Emotion.DIM -> R.string.emotion_dim
  Emotion.RESTLESS -> R.string.emotion_restless
  Emotion.CALM -> R.string.emotion_calm
  Emotion.HAPPY -> R.string.emotion_happy
  Emotion.ECSTATIC -> R.string.emotion_ecstatic
}

fun stageName(e: Stage) = when (e) {
  Stage.SPARK -> R.string.stage_spark
  Stage.FLAME -> R.string.stage_flame
  Stage.BONFIRE -> R.string.stage_bonfire
  Stage.LEGEND -> R.string.stage_legend
}
