package com.brasa.app.ui.pantallas

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
import com.brasa.app.ui.brasa.rememberAnimacionesActivas
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tokens

/** Duración de la parte "en la oscuridad" del arranque; el resto (nacer + anillo) ocurre ya en Inicio. Total ≤ 2 s. */
const val DURACION_ARRANQUE_MS = 1100

/**
 * Arranque: chispa que late → remolino de anillos violeta/magenta/fuego en sentidos opuestos → destello.
 * Un toque lo salta. Con "reducir animaciones" se omite.
 */
@Composable
fun Arranque(onFin: () -> Unit) {
  val animado = rememberAnimacionesActivas()
  val t = remember { Animatable(0f) }
  val desc = stringResource(R.string.arranque_descripcion)
  LaunchedEffect(Unit) {
    if (animado) t.animateTo(1f, tween(DURACION_ARRANQUE_MS, easing = LinearEasing))
    onFin()
  }
  val fondo = BrasaTema.colores.fondo
  Canvas(
    Modifier
      .fillMaxSize()
      .background(fondo)
      .semantics { contentDescription = desc }
      .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFin),
  ) {
    dibujarArranque(t.value)
  }
}

fun DrawScope.dibujarArranque(p: Float) {
  val c = center
  val s = size.minDimension
  fun tramo(a: Float, b: Float) = ((p - a) / (b - a)).coerceIn(0f, 1f)
  fun suave(x: Float) = 1f - (1f - x) * (1f - x) // ease-out

  // 1 · Chispa que late (0 – 0,3).
  val chispa = 1f - tramo(0.55f, 0.7f)
  if (chispa > 0f) {
    val latido = 1f + 0.35f * kotlin.math.sin(p * 40f)
    val r = s * 0.035f * latido * (0.6f + 0.4f * suave(tramo(0f, 0.25f)))
    drawCircle(
      brush = Brush.radialGradient(
        0f to Tokens.Colores.FuegoClaro, 0.3f to Tokens.Colores.Fuego.copy(alpha = 0.85f), 1f to Color.Transparent,
        center = c, radius = r * 4f,
      ),
      radius = r * 4f, center = c, alpha = chispa,
    )
  }

  // 2 · Remolino de energía (0,25 – 0,85): tres anillos girando en sentidos opuestos que se cierran.
  val entra = suave(tramo(0.22f, 0.45f))
  val colapso = suave(tramo(0.62f, 0.86f))
  if (entra > 0f && colapso < 1f) {
    val colores = listOf(Tokens.Colores.Violeta, Tokens.Colores.Magenta, Tokens.Colores.Fuego)
    colores.forEachIndexed { i, color ->
      val dir = if (i % 2 == 0) 1f else -1f
      val radio = s * (0.2f + 0.1f * i) * (1f - 0.92f * colapso) * (0.7f + 0.3f * entra)
      val giro = dir * p * 900f + i * 50f
      val barrido = 70f + 160f * entra
      val grosor = s * (0.03f - 0.006f * i) * (1f - 0.5f * colapso)
      rotate(giro, pivot = c) {
        drawArc(
          brush = Brush.sweepGradient(0f to Color.Transparent, barrido / 360f to color, center = c),
          startAngle = 0f, sweepAngle = barrido, useCenter = false,
          topLeft = Offset(c.x - radio, c.y - radio), size = Size(radio * 2f, radio * 2f),
          style = Stroke(grosor, cap = StrokeCap.Round), alpha = entra * (1f - colapso * 0.4f),
        )
      }
    }
  }

  // 3 · Destello (0,8 – 1): colapsa en luz blanca-dorada que se abre y se desvanece.
  val destello = tramo(0.8f, 1f)
  if (destello > 0f) {
    val r = s * (0.08f + 0.6f * suave(destello))
    drawCircle(
      brush = Brush.radialGradient(
        0f to Color.White, 0.25f to Tokens.Colores.Dorado, 0.6f to Tokens.Colores.Fuego.copy(alpha = 0.5f), 1f to Color.Transparent,
        center = c, radius = r,
      ),
      radius = r, center = c, alpha = 1f - destello * 0.6f,
    )
  }
}
