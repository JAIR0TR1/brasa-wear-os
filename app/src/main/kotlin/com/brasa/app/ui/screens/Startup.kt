package com.brasa.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.brasa.app.R
import com.brasa.app.ui.brasa.rememberAnimationsEnabled
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.Tokens

/** Duración de la parte "en la oscuridad" del arranque; el resto (nacer + anillo) ocurre ya en Inicio. Total ≤ 2 s. */
const val STARTUP_DURATION_MS = 1100

/**
 * Arranque: chispa que late → remolino de anillos violeta/magenta/fuego en sentidos opuestos → destello.
 * Un toque lo salta. Con "reducir animaciones" se omite.
 */
@Composable
fun StartupAnimation(onFinish: () -> Unit) {
  val animated = rememberAnimationsEnabled()
  val t = remember { Animatable(0f) }
  val description = stringResource(R.string.startup_description)
  LaunchedEffect(Unit) {
    if (animated) t.animateTo(1f, tween(STARTUP_DURATION_MS, easing = LinearEasing))
    onFinish()
  }
  val background = BrasaTheme.colors.background
  Canvas(
    Modifier
      .fillMaxSize()
      .background(background)
      .semantics { contentDescription = description }
      .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFinish),
  ) {
    drawStartup(t.value)
  }
}

fun DrawScope.drawStartup(p: Float) {
  val c = center
  val s = size.minDimension
  fun segment(a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
  fun easeOut(x: Float) = 1f - (1f - x) * (1f - x) // ease-out

  // 1 · Chispa que late (0 – 0,3).
  val spark = 1f - segment(0.55f, 0.7f)
  if (spark > 0f) {
    val beat = 1f + 0.35f * kotlin.math.sin(p * 40f)
    val r = s * 0.035f * beat * (0.6f + 0.4f * easeOut(segment(0f, 0.25f)))
    drawCircle(
      brush = Brush.radialGradient(
        0f to Tokens.Colors.LightFire, 0.3f to Tokens.Colors.Fire.copy(alpha = 0.85f), 1f to Color.Transparent,
        center = c, radius = r * 4f,
      ),
      radius = r * 4f, center = c, alpha = spark,
    )
  }

  // 2 · Remolino de energía (0,25 – 0,85): tres anillos girando en sentidos opuestos que se cierran.
  val entering = easeOut(segment(0.22f, 0.45f))
  val collapse = easeOut(segment(0.62f, 0.86f))
  if (entering > 0f && collapse < 1f) {
    val colors = listOf(Tokens.Colors.Violet, Tokens.Colors.Magenta, Tokens.Colors.Fire)
    colors.forEachIndexed { i, color ->
      val dir = if (i % 2 == 0) 1f else -1f
      val radius = s * (0.2f + 0.1f * i) * (1f - 0.92f * collapse) * (0.7f + 0.3f * entering)
      val spin = dir * p * 900f + i * 50f
      val sweep = 70f + 160f * entering
      val thickness = s * (0.03f - 0.006f * i) * (1f - 0.5f * collapse)
      rotate(spin, pivot = c) {
        drawArc(
          brush = Brush.sweepGradient(0f to Color.Transparent, sweep / 360f to color, center = c),
          startAngle = 0f, sweepAngle = sweep, useCenter = false,
          topLeft = Offset(c.x - radius, c.y - radius), size = Size(radius * 2f, radius * 2f),
          style = Stroke(thickness, cap = StrokeCap.Round), alpha = entering * (1f - collapse * 0.4f),
        )
      }
    }
  }

  // 3 · Destello (0,8 – 1): colapsa en luz blanca-dorada que se abre y se desvanece.
  val flash = segment(0.8f, 1f)
  if (flash > 0f) {
    val r = s * (0.08f + 0.6f * easeOut(flash))
    drawCircle(
      brush = Brush.radialGradient(
        0f to Color.White, 0.25f to Tokens.Colors.Gold, 0.6f to Tokens.Colors.Fire.copy(alpha = 0.5f), 1f to Color.Transparent,
        center = c, radius = r,
      ),
      radius = r, center = c, alpha = 1f - flash * 0.6f,
    )
  }
}
