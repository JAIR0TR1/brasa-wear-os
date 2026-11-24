package com.brasa.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.R
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.Stage
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.brasa.drawStar
import com.brasa.app.ui.brasa.stageName
import com.brasa.app.ui.brasa.rememberAnimationsEnabled
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val SPARKS = 10

/**
 * Subida de nivel (~2,6 s): la luz se abre en un anillo de fuego, diez chispas salen disparadas y
 * Brasa aparece eufórica con su etapa. Se cierra sola o al tocar.
 * Con "reducir animaciones": la misma pantalla quieta (halo y chispas fijas), el mismo tiempo, sin movimiento.
 *
 * @param progresoFijo congela la animación en ese punto (capturas).
 */
@Composable
fun LevelUp(level: Int, stage: Stage, newStage: Boolean, onFinish: () -> Unit, fixedProgress: Float? = null) {
  val colors = BrasaTheme.colors
  val animated = rememberAnimationsEnabled() && fixedProgress == null
  val p = remember { Animatable(if (animated) 0f else 0.55f) }
  LaunchedEffect(Unit) {
    if (fixedProgress != null) return@LaunchedEffect
    if (animated) p.animateTo(1f, tween(Tokens.Duration.levelUp, easing = LinearEasing))
    else delay(Tokens.Duration.levelUp.toLong())
    onFinish()
  }
  val t = fixedProgress ?: p.value
  // Entra y sale con un fundido corto; en reposo (reducir animaciones) siempre visible.
  val opacity = if (!animated) 1f else when {
    t < 0.1f -> t / 0.1f
    t > 0.88f -> ((1f - t) / 0.12f).coerceIn(0f, 1f)
    else -> 1f
  }
  // Estallido: 0.08 → 0.6, con salida rápida (ease-out cúbica).
  val burst = ((t - 0.08f) / 0.52f).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) * (1f - it) }
  // Brasa crece con un pequeño rebote al final.
  val growth = ((t - 0.05f) / 0.4f).coerceIn(0f, 1f)
  val scale = 0.6f + 0.4f * growth + 0.08f * sin(growth * Math.PI.toFloat())
  val gold = if (colors.dark) Tokens.Colors.Gold else Tokens.Colors.LightThemeFire
  val title = stringResource(R.string.level_up_title, level)
  val subtitle = if (newStage) stringResource(R.string.stage_reached, stringResource(stageName(stage)))
    else stringResource(R.string.level_up_voice)

  EmotionBackground(
    Emotion.ECSTATIC,
    Modifier
      .graphicsLayer { alpha = opacity }
      .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFinish)
      // TalkBack lo anuncia al aparecer.
      .clearAndSetSemantics { contentDescription = "$title. $subtitle"; liveRegion = LiveRegionMode.Assertive },
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val r = size.minDimension / 2f
      val maxR = r - Tokens.Spacing.safeEdge.toPx()
      // Anillo de fuego que se abre (degradado radial: sin blur).
      val radius = maxR * (0.3f + 0.7f * burst)
      val ringOpacity = (1f - burst * 0.85f)
      drawCircle(
        Brush.radialGradient(
          0f to Color.Transparent, 0.72f to Color.Transparent,
          0.9f to gold.copy(alpha = 0.55f * ringOpacity), 1f to Color.Transparent,
          center = center, radius = radius,
        ),
        radius = radius,
      )
      drawCircle(gold.copy(alpha = 0.5f * ringOpacity), radius = radius * 0.93f, style = Stroke(2.dp.toPx()))
      // Chispas que salen hacia el borde y se apagan al llegar.
      for (i in 0 until SPARKS) {
        val angle = Math.toRadians(i * 360.0 / SPARKS - 72.0)
        val d = maxR * (0.32f + 0.6f * burst) * (if (i % 2 == 0) 1f else 0.82f)
        val c = Offset(center.x + (cos(angle) * d).toFloat(), center.y + (sin(angle) * d).toFloat())
        val starSize = (if (i % 2 == 0) 5.5f else 4f).dp.toPx() * (1f - 0.4f * burst)
        drawStar(c, starSize, if (i % 3 == 0) Tokens.Colors.LightFire else gold, (1.1f - burst).coerceIn(0.25f, 1f))
      }
    }
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Spacing.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.weight(0.3f))
      Brasa(
        Emotion.ECSTATIC, stage,
        Modifier.fillMaxWidth(0.62f).weight(1f).graphicsLayer { scaleX = scale; scaleY = scale },
        fixedTime = if (fixedProgress != null) 1.3f else null,
      )
      Spacer(Modifier.height(Tokens.Spacing.xs))
      Text(title, style = BrasaType.title, color = gold, maxLines = 1)
      Text(
        subtitle, style = BrasaType.voice, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = Tokens.Spacing.s),
      )
      Spacer(Modifier.weight(0.32f))
    }
  }
}
