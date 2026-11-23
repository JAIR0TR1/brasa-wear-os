package com.brasa.app.ui.pantallas

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
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.brasa.dibujarEstrella
import com.brasa.app.ui.brasa.nombreEtapa
import com.brasa.app.ui.brasa.rememberAnimacionesActivas
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val CHISPAS = 10

/**
 * Subida de nivel (~2,6 s): la luz se abre en un anillo de fuego, diez chispas salen disparadas y
 * Brasa aparece eufórica con su etapa. Se cierra sola o al tocar.
 * Con "reducir animaciones": la misma pantalla quieta (halo y chispas fijas), el mismo tiempo, sin movimiento.
 *
 * @param progresoFijo congela la animación en ese punto (capturas).
 */
@Composable
fun SubidaNivel(nivel: Int, etapa: Etapa, etapaNueva: Boolean, onFin: () -> Unit, progresoFijo: Float? = null) {
  val colores = BrasaTema.colores
  val animado = rememberAnimacionesActivas() && progresoFijo == null
  val p = remember { Animatable(if (animado) 0f else 0.55f) }
  LaunchedEffect(Unit) {
    if (progresoFijo != null) return@LaunchedEffect
    if (animado) p.animateTo(1f, tween(Tokens.Duracion.subidaNivel, easing = LinearEasing))
    else delay(Tokens.Duracion.subidaNivel.toLong())
    onFin()
  }
  val t = progresoFijo ?: p.value
  // Entra y sale con un fundido corto; en reposo (reducir animaciones) siempre visible.
  val opacidad = if (!animado) 1f else when {
    t < 0.1f -> t / 0.1f
    t > 0.88f -> ((1f - t) / 0.12f).coerceIn(0f, 1f)
    else -> 1f
  }
  // Estallido: 0.08 → 0.6, con salida rápida (ease-out cúbica).
  val estallido = ((t - 0.08f) / 0.52f).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) * (1f - it) }
  // Brasa crece con un pequeño rebote al final.
  val crece = ((t - 0.05f) / 0.4f).coerceIn(0f, 1f)
  val escala = 0.6f + 0.4f * crece + 0.08f * sin(crece * Math.PI.toFloat())
  val oro = if (colores.oscuro) Tokens.Colores.Dorado else Tokens.Colores.FuegoClaroTema
  val titulo = stringResource(R.string.nivel_subido, nivel)
  val subtitulo = if (etapaNueva) stringResource(R.string.etapa_alcanzada, stringResource(nombreEtapa(etapa)))
    else stringResource(R.string.nivel_subido_voz)

  FondoEmocion(
    Emocion.EUFORICA,
    Modifier
      .graphicsLayer { alpha = opacidad }
      .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onFin)
      // TalkBack lo anuncia al aparecer.
      .clearAndSetSemantics { contentDescription = "$titulo. $subtitulo"; liveRegion = LiveRegionMode.Assertive },
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val r = size.minDimension / 2f
      val maxR = r - Tokens.Espacio.bordeSeguro.toPx()
      // Anillo de fuego que se abre (degradado radial: sin blur).
      val radio = maxR * (0.3f + 0.7f * estallido)
      val alfaAnillo = (1f - estallido * 0.85f)
      drawCircle(
        Brush.radialGradient(
          0f to Color.Transparent, 0.72f to Color.Transparent,
          0.9f to oro.copy(alpha = 0.55f * alfaAnillo), 1f to Color.Transparent,
          center = center, radius = radio,
        ),
        radius = radio,
      )
      drawCircle(oro.copy(alpha = 0.5f * alfaAnillo), radius = radio * 0.93f, style = Stroke(2.dp.toPx()))
      // Chispas que salen hacia el borde y se apagan al llegar.
      for (i in 0 until CHISPAS) {
        val ang = Math.toRadians(i * 360.0 / CHISPAS - 72.0)
        val d = maxR * (0.32f + 0.6f * estallido) * (if (i % 2 == 0) 1f else 0.82f)
        val c = Offset(center.x + (cos(ang) * d).toFloat(), center.y + (sin(ang) * d).toFloat())
        val tam = (if (i % 2 == 0) 5.5f else 4f).dp.toPx() * (1f - 0.4f * estallido)
        dibujarEstrella(c, tam, if (i % 3 == 0) Tokens.Colores.FuegoClaro else oro, (1.1f - estallido).coerceIn(0.25f, 1f))
      }
    }
    Column(
      Modifier.fillMaxSize().padding(horizontal = Tokens.Espacio.xl),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.weight(0.3f))
      Brasa(
        Emocion.EUFORICA, etapa,
        Modifier.fillMaxWidth(0.62f).weight(1f).graphicsLayer { scaleX = escala; scaleY = escala },
        tiempoFijo = if (progresoFijo != null) 1.3f else null,
      )
      Spacer(Modifier.height(Tokens.Espacio.xs))
      Text(titulo, style = Tipo.titulo, color = oro, maxLines = 1)
      Text(
        subtitulo, style = Tipo.voz, color = colores.texto, maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = Tokens.Espacio.s),
      )
      Spacer(Modifier.weight(0.32f))
    }
  }
}
