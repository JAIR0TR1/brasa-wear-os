package com.brasa.app.ui.componentes

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
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anillo que abraza el borde con un punto de luz en la punta.
 * Se dibuja desde 0 hasta [progreso] en ~900 ms al entrar o al cambiar.
 */
@Composable
fun AnilloBorde(
  progreso: Float,
  color: Color,
  modifier: Modifier = Modifier,
  grosor: Dp = 6.dp,
  margen: Dp = 4.dp,
  animado: Boolean = true,
  descripcion: String? = null,
) {
  val colores = BrasaTema.colores
  val anim = remember { Animatable(if (animado) 0f else progreso) }
  LaunchedEffect(progreso, animado) {
    if (animado) anim.animateTo(progreso.coerceIn(0f, 1f), tween(Tokens.Duracion.dibujo, easing = FastOutSlowInEasing))
    else anim.snapTo(progreso.coerceIn(0f, 1f))
  }
  val sem = if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier
  Canvas(modifier.fillMaxSize().then(sem)) {
    val g = grosor.toPx()
    val inset = g / 2f + margen.toPx()
    val lado = size.minDimension - inset * 2f
    val topLeft = Offset((size.width - lado) / 2f, (size.height - lado) / 2f)
    val arco = Size(lado, lado)
    drawArc(colores.pista, 0f, 360f, false, topLeft, arco, style = Stroke(g))
    val p = anim.value
    // Por debajo del 1 % no se dibuja arco ni punto de luz (no hay progreso que señalar).
    if (p < 0.01f) return@Canvas
    val inicio = lerp(color, colores.fondo, if (colores.oscuro) 0.55f else 0.35f)
    rotate(-90f) {
      drawArc(
        brush = Brush.sweepGradient(0f to inicio, p.coerceAtLeast(0.01f) to color, center = center),
        startAngle = 0f, sweepAngle = 360f * p, useCenter = false,
        topLeft = topLeft, size = arco, style = Stroke(g, cap = StrokeCap.Round),
      )
    }
    // Punto de luz en la punta (degradado radial, sin blur).
    val ang = Math.toRadians((-90f + 360f * p).toDouble())
    val r = lado / 2f
    val punta = Offset(center.x + (cos(ang) * r).toFloat(), center.y + (sin(ang) * r).toFloat())
    val luz = if (colores.oscuro) Tokens.Colores.FuegoClaro else color
    drawCircle(
      brush = Brush.radialGradient(0f to luz.copy(alpha = 0.85f), 1f to Color.Transparent, center = punta, radius = g * 2.4f),
      radius = g * 2.4f, center = punta,
    )
    drawCircle(if (colores.oscuro) Tokens.Colores.FuegoClaro else Color.White, radius = g * 0.38f, center = punta)
  }
}

/**
 * Botón píldora de Brasa: degradado de fuego, 48 dp de alto mínimo, icono dibujado.
 */
@Composable
fun BotonBrasa(
  texto: String,
  color: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icono: (@Composable () -> Unit)? = null,
) {
  val colores = BrasaTema.colores
  val textoColor = if (colores.oscuro) Tokens.Colores.Negro else Color.White
  Row(
    modifier
      .defaultMinSize(minHeight = Tokens.Espacio.tactil)
      .clip(CircleShape)
      .background(Brush.horizontalGradient(listOf(lerp(color, Tokens.Colores.FuegoClaro, if (colores.oscuro) 0.18f else 0f), color)))
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = Tokens.Espacio.l + Tokens.Espacio.xs),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icono != null) {
      Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) { icono() }
      Spacer(Modifier.width(Tokens.Espacio.s))
    }
    Text(texto, style = Tipo.etiqueta, color = textoColor, maxLines = 1)
  }
}

/** Icono de respiración: dos anillos concéntricos (ondas de aire). */
@Composable
fun IconoRespirar(color: Color) {
  Canvas(Modifier.fillMaxSize()) {
    val s = size.minDimension
    drawCircle(color, radius = s * 0.18f)
    drawCircle(color, radius = s * 0.34f, style = Stroke(s * 0.09f))
    drawCircle(color.copy(alpha = 0.6f), radius = s * 0.48f, style = Stroke(s * 0.07f))
  }
}

/** Fila de ajuste: etiqueta + valor en acento, borde sutil con brillo. Toda la fila es táctil (≥ 48 dp). */
@Composable
fun FilaAjuste(
  etiqueta: String,
  valor: String,
  acento: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colores = BrasaTema.colores
  val forma = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
  Row(
    modifier
      .defaultMinSize(minHeight = 52.dp)
      .clip(forma)
      .background(
        Brush.verticalGradient(
          listOf(acento.copy(alpha = if (colores.oscuro) 0.16f else 0.1f), acento.copy(alpha = if (colores.oscuro) 0.05f else 0.03f)),
        ),
      )
      .border(1.dp, acento.copy(alpha = 0.3f), forma)
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = Tokens.Espacio.l, vertical = Tokens.Espacio.s),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      Modifier.size(8.dp).clip(CircleShape)
        .background(Brush.radialGradient(listOf(Tokens.Colores.FuegoClaro, acento))),
    )
    Spacer(Modifier.width(Tokens.Espacio.s + Tokens.Espacio.xs))
    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
      Text(etiqueta, style = Tipo.etiqueta.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Start), color = colores.texto, maxLines = 1)
      Text(valor, style = Tipo.dato.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Start), color = acento, maxLines = 1)
    }
  }
}
