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
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/** true si el sistema NO pide reducir animaciones (escala de animación > 0). */
@Composable
fun rememberAnimacionesActivas(): Boolean {
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
  emocion: Emocion,
  etapa: Etapa,
  modifier: Modifier = Modifier,
  reaccion: Int = 0,
  animado: Boolean = rememberAnimacionesActivas(),
  tiempoFijo: Float? = null,
) {
  val oscuro = BrasaTema.colores.oscuro
  val objetivo = remember(emocion) { poseDe(emocion) }
  val objetivoForma = remember(etapa) { formaDe(etapa) }

  var desde by remember { mutableStateOf(objetivo) }
  var hacia by remember { mutableStateOf(objetivo) }
  val progreso = remember { Animatable(1f) }
  var desdeForma by remember { mutableStateOf(objetivoForma) }
  var haciaForma by remember { mutableStateOf(objetivoForma) }
  val progresoForma = remember { Animatable(1f) }
  val rebote = remember { Animatable(1f) }
  val risa = remember { Animatable(0f) }
  val parpadeo = remember { Animatable(0f) }

  // Transición entre emociones: rebote (sale y entra) + interpolación de toda la pose.
  LaunchedEffect(emocion) {
    if (hacia == objetivo) return@LaunchedEffect
    desde = lerpPose(desde, hacia, progreso.value)
    hacia = objetivo
    progreso.snapTo(0f)
    if (!animado) { progreso.snapTo(1f); return@LaunchedEffect }
    launch {
      rebote.animateTo(0.84f, tween(Tokens.Duracion.rapida))
      rebote.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
    }
    progreso.animateTo(1f, tween(Tokens.Duracion.media + 200, easing = FastOutSlowInEasing))
  }

  LaunchedEffect(etapa) {
    if (haciaForma == objetivoForma) return@LaunchedEffect
    desdeForma = lerpForma(desdeForma, haciaForma, progresoForma.value)
    haciaForma = objetivoForma
    progresoForma.snapTo(0f)
    if (!animado) { progresoForma.snapTo(1f); return@LaunchedEffect }
    launch {
      rebote.animateTo(1.15f, tween(Tokens.Duracion.media))
      rebote.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
    }
    progresoForma.animateTo(1f, tween(Tokens.Duracion.dibujo, easing = FastOutSlowInEasing))
  }

  // Reacción al toque: rebote y risa breve.
  LaunchedEffect(reaccion) {
    if (reaccion == 0) return@LaunchedEffect
    launch {
      rebote.snapTo(0.9f)
      rebote.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = 500f))
    }
    risa.animateTo(1f, tween(Tokens.Duracion.rapida))
    delay(700)
    risa.animateTo(0f, tween(Tokens.Duracion.media))
  }

  val ciclo by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
  val activo = animado && tiempoFijo == null && ciclo.isAtLeast(Lifecycle.State.RESUMED)

  // Parpadeo aleatorio cada 3–6 s.
  LaunchedEffect(activo) {
    while (activo) {
      delay(Random.nextLong(3000, 6000))
      parpadeo.animateTo(1f, tween(70))
      parpadeo.animateTo(0f, tween(110))
    }
  }

  val tiempo = remember { mutableFloatStateOf(tiempoFijo ?: 0f) }
  LaunchedEffect(activo) {
    if (!activo) return@LaunchedEffect
    val base = tiempo.floatValue
    val inicio = withFrameNanos { it }
    while (true) {
      withFrameNanos { tiempo.floatValue = base + (it - inicio) / 1_000_000_000f }
    }
  }

  val descripcion = stringResource(
    R.string.brasa_descripcion,
    stringResource(nombreEmocion(emocion)),
    stringResource(nombreEtapa(etapa)),
  )
  Canvas(modifier.semantics { contentDescription = descripcion }) {
    dibujarBrasa(
      pose = lerpPose(desde, hacia, progreso.value),
      forma = lerpForma(desdeForma, haciaForma, progresoForma.value),
      t = tiempo.floatValue,
      parpadeo = parpadeo.value,
      rebote = rebote.value,
      risa = risa.value,
      oscuro = oscuro,
    )
  }
}

/**
 * Fondo de cada emoción: degradado radial del color de la emoción que se desvanece
 * a negro puro en los bordes (OLED: los píxeles negros no consumen).
 */
@Composable
fun FondoEmocion(
  emocion: Emocion,
  modifier: Modifier = Modifier,
  intensidad: Float = 1f,
  content: @Composable BoxScope.() -> Unit,
) {
  val colores = BrasaTema.colores
  val objetivo = Tokens.paleta(emocion, colores.oscuro).base
  val color by animateColorAsState(objetivo, tween(Tokens.Duracion.transicionFondo), label = "fondo")
  Box(
    modifier
      .fillMaxSize()
      .drawBehind {
        drawRect(colores.fondo)
        val a = (if (colores.oscuro) 0.42f else 0.2f) * intensidad
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

fun nombreEmocion(e: Emocion) = when (e) {
  Emocion.APAGADA -> R.string.emocion_apagada
  Emocion.INQUIETA -> R.string.emocion_inquieta
  Emocion.TRANQUILA -> R.string.emocion_tranquila
  Emocion.FELIZ -> R.string.emocion_feliz
  Emocion.EUFORICA -> R.string.emocion_euforica
}

fun nombreEtapa(e: Etapa) = when (e) {
  Etapa.CHISPA -> R.string.etapa_chispa
  Etapa.LLAMA -> R.string.etapa_llama
  Etapa.FOGATA -> R.string.etapa_fogata
  Etapa.LEYENDA -> R.string.etapa_leyenda
}
