package com.brasa.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anillo que abraza el borde con un punto de luz en la punta.
 * Se dibuja desde 0 hasta [progreso] en ~900 ms al entrar o al cambiar.
 */
@Composable
fun EdgeRing(
  progress: Float,
  color: Color,
  modifier: Modifier = Modifier,
  thickness: Dp = 6.dp,
  margin: Dp = 4.dp,
  animated: Boolean = true,
  description: String? = null,
) {
  val colors = BrasaTheme.colors
  val anim = remember { Animatable(if (animated) 0f else progress) }
  LaunchedEffect(progress, animated) {
    if (animated) anim.animateTo(progress.coerceIn(0f, 1f), tween(Tokens.Duration.draw, easing = FastOutSlowInEasing))
    else anim.snapTo(progress.coerceIn(0f, 1f))
  }
  val descriptionModifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
  Canvas(modifier.fillMaxSize().then(descriptionModifier)) {
    val g = thickness.toPx()
    val inset = g / 2f + margin.toPx()
    val side = size.minDimension - inset * 2f
    val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
    val arc = Size(side, side)
    drawArc(colors.track, 0f, 360f, false, topLeft, arc, style = Stroke(g))
    val p = anim.value
    // Por debajo del 1 % no se dibuja arco ni punto de luz (no hay progreso que señalar).
    if (p < 0.01f) return@Canvas
    val start = lerp(color, colors.background, if (colors.dark) 0.55f else 0.35f)
    rotate(-90f) {
      drawArc(
        brush = Brush.sweepGradient(0f to start, p.coerceAtLeast(0.01f) to color, center = center),
        startAngle = 0f, sweepAngle = 360f * p, useCenter = false,
        topLeft = topLeft, size = arc, style = Stroke(g, cap = StrokeCap.Round),
      )
    }
    // Punto de luz en la punta (degradado radial, sin blur).
    val angle = Math.toRadians((-90f + 360f * p).toDouble())
    val r = side / 2f
    val tip = Offset(center.x + (cos(angle) * r).toFloat(), center.y + (sin(angle) * r).toFloat())
    val light = if (colors.dark) Tokens.Colors.LightFire else color
    drawCircle(
      brush = Brush.radialGradient(0f to light.copy(alpha = 0.85f), 1f to Color.Transparent, center = tip, radius = g * 2.4f),
      radius = g * 2.4f, center = tip,
    )
    drawCircle(if (colors.dark) Tokens.Colors.LightFire else Color.White, radius = g * 0.38f, center = tip)
  }
}

/**
 * Botón píldora de Brasa: degradado de fuego, 48 dp de alto mínimo, icono dibujado.
 */
@Composable
fun BrasaButton(
  text: String,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: (@Composable () -> Unit)? = null,
) {
  val colors = BrasaTheme.colors
  val textColor = if (colors.dark) Tokens.Colors.Black else Color.White
  Row(
    modifier
      .defaultMinSize(minWidth = Tokens.Spacing.touchTarget * 2, minHeight = Tokens.Spacing.touchTarget)
      .clip(CircleShape)
      .background(Brush.horizontalGradient(listOf(lerp(color, Tokens.Colors.LightFire, if (colors.dark) 0.18f else 0f), color)))
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = Tokens.Spacing.l + Tokens.Spacing.xs),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
  ) {
    if (icon != null) {
      Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { icon() }
      Spacer(Modifier.width(Tokens.Spacing.s))
    }
    Text(text, style = BrasaType.label, color = textColor, maxLines = 1)
  }
}

/** Icono de respiración: dos anillos concéntricos (ondas de aire). */
@Composable
fun BreatheIcon(color: Color) {
  Canvas(Modifier.fillMaxSize()) {
    val s = size.minDimension
    drawCircle(color, radius = s * 0.18f)
    drawCircle(color, radius = s * 0.34f, style = Stroke(s * 0.09f))
    drawCircle(color.copy(alpha = 0.6f), radius = s * 0.48f, style = Stroke(s * 0.07f))
  }
}

/** Fila de ajuste: etiqueta + valor en acento, borde sutil con brillo. Toda la fila es táctil (≥ 48 dp). */
@Composable
fun SettingRow(
  label: String,
  value: String,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BrasaTheme.colors
  val shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
  Row(
    modifier
      .defaultMinSize(minHeight = 52.dp)
      .clip(shape)
      .background(
        Brush.verticalGradient(
          listOf(accent.copy(alpha = if (colors.dark) 0.16f else 0.1f), accent.copy(alpha = if (colors.dark) 0.05f else 0.03f)),
        ),
      )
      .border(1.dp, accent.copy(alpha = 0.3f), shape)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = Tokens.Spacing.l, vertical = Tokens.Spacing.s),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier.size(8.dp).clip(CircleShape)
        .background(Brush.radialGradient(listOf(Tokens.Colors.LightFire, accent))),
    )
    Spacer(Modifier.width(Tokens.Spacing.s + Tokens.Spacing.xs))
    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
      Text(label, style = BrasaType.label.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Start), color = colors.text, maxLines = 1)
      Text(value, style = BrasaType.data.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Start), color = accent, maxLines = 1)
    }
  }
}
