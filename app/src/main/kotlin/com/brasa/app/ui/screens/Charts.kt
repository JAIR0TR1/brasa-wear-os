package com.brasa.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.brasa.app.BrasaState
import com.brasa.app.R
import com.brasa.app.ui.brasa.Emotion
import com.brasa.app.ui.brasa.EmotionBackground
import com.brasa.app.ui.brasa.rememberAnimationsEnabled
import com.brasa.app.ui.components.EdgeTitle
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import kotlin.math.cos
import kotlin.math.sin

/** Progreso de "dibujado al entrar" (~900 ms, ease-out). Con animaciones reducidas aparece directo. */
@Composable
private fun rememberEntrance(key: Any? = Unit, fixed: Boolean = false): Float {
  val animated = rememberAnimationsEnabled() && !fixed
  val p = remember(key) { Animatable(if (animated) 0f else 1f) }
  LaunchedEffect(key) { if (animated) p.animateTo(1f, tween(Tokens.Duration.draw, easing = FastOutSlowInEasing)) }
  return p.value
}

// ---------------------------------------------------------------- 1 · Pasos por hora (barras radiales)

@Composable
fun StepsChart(state: BrasaState, currentHour: Int = LocalTime.now().hour, fixed: Boolean = false) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(Emotion.HAPPY, colors.dark).accent
  val entrance = rememberEntrance(fixed = fixed)
  val locale = LocalConfiguration.current.locales[0]
  val total = remember(state.steps, locale) { NumberFormat.getIntegerInstance(locale).format(state.steps) }
  val data = state.stepsPerHour
  val max = (data.maxOrNull() ?: 0).coerceAtLeast(1)
  val peak = data.indices.maxByOrNull { data[it] } ?: 0
  val hasData = data.any { it > 0 }
  val description = stringResource(R.string.steps_chart_description, state.steps, peak)

  EmotionBackground(Emotion.HAPPY, intensity = 0.55f) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
      val c = center
      // Margen seguro: ni la barra más larga ni su brillo llegan al borde (ni al indicador de páginas).
      val rExt = size.minDimension / 2f - Tokens.Spacing.safeEdge.toPx()
      val maxLength = rExt * 0.32f
      val rInt = rExt - maxLength
      val thickness = 4.5.dp.toPx()
      for (h in 0 until 24) {
        // Escalonado: cada barra empieza un poco después (barrido horario).
        val local = ((entrance * 1.4f) - h / 24f * 0.4f).coerceIn(0f, 1f)
        val angle = Math.toRadians((-90.0 + h * 15.0))
        val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
        val frac = data[h].toFloat() / max
        val length = (thickness * 0.6f + maxLength * frac) * local
        val a = c + dir * rInt
        val b = c + dir * (rInt + length)
        val isNow = h == currentHour
        val color = when {
          isNow -> if (colors.dark) Tokens.Colors.LightFire else Tokens.Colors.LightThemeFire
          h > currentHour -> colors.track
          else -> lerp(lerp(accent, colors.background, 0.6f), accent, frac.coerceIn(0.25f, 1f))
        }
        if (isNow) {
          drawCircle(
            brush = Brush.radialGradient(0f to color.copy(alpha = 0.6f), 1f to Color.Transparent, center = b, radius = thickness * 2.4f),
            radius = thickness * 2.4f, center = b,
          )
        }
        drawLine(color, a, b, strokeWidth = thickness, cap = StrokeCap.Round)
      }
    }
    // Centro: su ancho sale del radio interior real del anillo de barras (cabe en 192 dp, cuadrado y letra grande).
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      val rInt = (minOf(maxWidth, maxHeight) / 2 - Tokens.Spacing.safeEdge) * (1f - 0.32f)
      // La línea de "pico" queda bajo el centro, donde la cuerda del anillo interior es más corta.
      val width = rInt * 2 * 0.7f
      Column(Modifier.width(width).clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.axis_hour, 0), style = BrasaType.data.copy(fontSize = 10.sp), color = colors.secondaryText, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        BasicText(
          total, style = BrasaType.figure.copy(color = colors.text), maxLines = 1,
          autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 26.sp),
        )
        BasicText(
          stringResource(R.string.steps_today), style = BrasaType.data.copy(color = colors.secondaryText), maxLines = 1,
          autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = BrasaType.data.fontSize),
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
          if (hasData) stringResource(R.string.peak_at, "%02d:00".format(peak)) else stringResource(R.string.no_steps_yet),
          style = BrasaType.label.copy(color = accent), maxLines = 1,
          autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 11.sp),
        )
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.axis_hour, 12), style = BrasaType.data.copy(fontSize = 10.sp), color = colors.secondaryText, maxLines = 1)
      }
    }
  }
}

// ---------------------------------------------------------------- 2 · Pulso del día (línea + área)

@Composable
fun HeartRateChart(state: BrasaState, fixed: Boolean = false) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(Emotion.RESTLESS, colors.dark).accent
  val calmColor = if (colors.dark) Tokens.Colors.Water else Tokens.Colors.LightThemeWater
  val entrance = rememberEntrance(fixed = fixed)
  val zone = ZoneId.systemDefault()
  val dayStartMs = remember { LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli() }
  val samples = state.heartRates
  val bpm = samples.map { it.bpm }
  val resting = if (bpm.size >= 10) bpm.sorted()[(bpm.size * 0.2).toInt()] else bpm.minOrNull()
  val maxValue = bpm.maxOrNull()
  val breathSession = state.breathSessions.lastOrNull()
  val description = if (maxValue != null) stringResource(R.string.heart_rate_chart_description, resting ?: 0, maxValue) else stringResource(R.string.no_heart_rate_yet)

  val measurer = rememberTextMeasurer()
  if (samples.isEmpty()) {
    EmptyState(stringResource(R.string.heart_rate_today), stringResource(R.string.no_heart_rate_yet), stringResource(R.string.no_heart_rate_hint), Emotion.RESTLESS)
    return
  }
  // Pocos datos reales (recién instalada o reloj recién puesto): una línea de 2 puntos no dice nada.
  if (samples.size < MIN_HEART_RATE_SAMPLES || samples.last().ms - samples.first().ms < MIN_HEART_RATE_SPAN_MS) {
    EmptyState(
      stringResource(R.string.heart_rate_today), stringResource(R.string.heart_rate_measuring),
      stringResource(R.string.heart_rate_measuring_hint, samples.maxBy { it.ms }.bpm), Emotion.RESTLESS,
    )
    return
  }
  EmotionBackground(Emotion.RESTLESS, intensity = 0.45f) {
    EdgeTitle(stringResource(R.string.heart_rate_today))
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(Modifier.height(40.dp))
      Text(
        if (maxValue != null) stringResource(R.string.rest_max, resting ?: 0, maxValue) else stringResource(R.string.no_heart_rate_yet),
        style = BrasaType.data, color = colors.secondaryText, maxLines = 1,
      )
      Spacer(Modifier.height(6.dp))
      Canvas(Modifier.fillMaxWidth().weight(1f).semantics { contentDescription = description }) {
        // Margen lateral: abajo el círculo se estrecha; el eje y sus etiquetas quedan dentro.
        val side = AXIS_MARGIN.toPx()
        val area = Rect(side, 14.dp.toPx(), size.width - side, size.height - 16.dp.toPx())
        // Línea base y guía de reposo + 20 (umbral de tensión), discontinua.
        drawLine(colors.track, Offset(area.left, area.bottom), Offset(area.right, area.bottom), strokeWidth = 1.dp.toPx())
        if (samples.size < 2) return@Canvas
        val minY = (bpm.min() - 8).coerceAtLeast(40)
        // El umbral de tensión (reposo + 20) siempre cabe en el gráfico, aunque el día haya sido tranquilo.
        val threshold = resting?.let { it + com.brasa.app.domain.Rules.TENSION_MARGIN_BPM } ?: 0
        val maxY = maxOf(bpm.max(), threshold) + 8
        fun x(ms: Long) = area.left + ((ms - dayStartMs) / 86_400_000f).coerceIn(0f, 1f) * area.width
        fun y(v: Int) = area.bottom - (v - minY).toFloat() / (maxY - minY) * area.height
        if (resting != null) {
          val yu = y(resting + com.brasa.app.domain.Rules.TENSION_MARGIN_BPM)
          drawLine(
            colors.secondaryText.copy(alpha = 0.45f), Offset(area.left, yu), Offset(area.right, yu),
            strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
          )
        }
        // Máximo ~96 puntos: suficiente para un reloj y barato de dibujar.
        val stride = (samples.size / 96).coerceAtLeast(1)
        val points = samples.filterIndexed { i, _ -> i % stride == 0 || i == samples.lastIndex }.map { Offset(x(it.ms), y(it.bpm)) }
        val line = Path().apply { points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        val fillPath = Path().apply {
          addPath(line); lineTo(points.last().x, area.bottom); lineTo(points.first().x, area.bottom); close()
        }
        // Se revela de izquierda a derecha al entrar.
        clipRect(right = area.left + (area.width + 20f) * entrance) {
          drawPath(fillPath, Brush.verticalGradient(listOf(accent.copy(alpha = 0.5f), accent.copy(alpha = 0f)), startY = area.top, endY = area.bottom))
          drawPath(line, accent, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
          val maxIndex = samples.indexOfFirst { it.bpm == maxValue }
          if (maxIndex >= 0) {
            val p = Offset(x(samples[maxIndex].ms), y(samples[maxIndex].bpm))
            drawCircle(accent, 4.dp.toPx(), p)
            drawCircle(colors.background, 1.8.dp.toPx(), p)
            // Cifra del pico encima del punto.
            val label = measurer.measure("${samples[maxIndex].bpm}", BrasaType.data.copy(color = accent))
            drawText(label, topLeft = Offset(p.x - label.size.width / 2f, p.y - label.size.height - 5.dp.toPx()))
          }
          if (breathSession != null) {
            val rx = x(breathSession.ms)
            drawLine(calmColor.copy(alpha = 0.7f), Offset(rx, area.top), Offset(rx, area.bottom), strokeWidth = 1.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f)))
            val nearest = samples.minByOrNull { kotlin.math.abs(it.ms - breathSession.ms) }
            if (nearest != null) {
              val p = Offset(rx, y(nearest.bpm))
              drawCircle(calmColor, 5.dp.toPx(), p)
              drawCircle(colors.background, 2.dp.toPx(), p)
            }
          }
        }
        // Eje: 0 h · 12 h · 24 h (texto en Compose abajo).
      }
      Box(Modifier.fillMaxWidth().padding(horizontal = AXIS_MARGIN)) {
        Text(stringResource(R.string.axis_hour, 0), style = BrasaType.data, color = colors.secondaryText, modifier = Modifier.align(Alignment.CenterStart))
        Text(stringResource(R.string.axis_hour, 12), style = BrasaType.data, color = colors.secondaryText, modifier = Modifier.align(Alignment.Center))
        Text(stringResource(R.string.axis_hour, 24), style = BrasaType.data, color = colors.secondaryText, modifier = Modifier.align(Alignment.CenterEnd))
      }
      Spacer(Modifier.height(6.dp))
      // Chip del evento de respiración (mismo color de "calma" que el marcador).
      val decrease = breathSession?.let { r -> if (r.before != null && r.after != null) r.after - r.before else null }
      // Solo se destaca si el pulso bajó (no se presenta una subida como logro).
      if (decrease != null && decrease < 0) {
        Box(
          Modifier.padding(horizontal = 10.dp).height(30.dp).clip(CircleShape).background(calmColor.copy(alpha = if (colors.dark) 0.16f else 0.12f))
            .padding(horizontal = 12.dp),
          contentAlignment = Alignment.Center,
        ) {
          // Se ajusta al ancho disponible (fuente grande, pantalla pequeña) en lugar de cortarse.
          BasicText(
            stringResource(R.string.after_breathing, "−${-decrease}"), style = BrasaType.label.copy(color = calmColor), maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 12.sp),
          )
        }
      } else {
        // Mismo alto que el chip: el eje no se desplaza hacia el borde.
        Spacer(Modifier.height(30.dp))
      }
      Spacer(Modifier.height(26.dp))
    }
  }
}

// ---------------------------------------------------------------- 3 · Semana contra la meta adaptativa

/** Frase local que explica la meta (la fase 5 la sustituye por la de la IA cuando hay red). */
@Composable
fun localGoalPhrase(goal: Int, previousEmotion: Int): String = when {
  goal > previousEmotion -> stringResource(R.string.goal_up)
  goal < previousEmotion -> stringResource(R.string.goal_down)
  else -> stringResource(R.string.goal_same)
}

@Composable
fun WeekChart(state: BrasaState, goalPhrase: String? = null, fixed: Boolean = false, today: LocalDate = LocalDate.now()) {
  val colors = BrasaTheme.colors
  val accent = Tokens.palette(Emotion.HAPPY, colors.dark).accent
  val entrance = rememberEntrance(fixed = fixed)
  val locale = LocalConfiguration.current.locales[0]
  // Siempre 7 huecos (los días sin registro quedan vacíos): la barra de hoy nunca ocupa todo el ancho.
  val previousDays = state.history.takeLast(6)
  val days = List(6 - previousDays.size) { -1 } + previousDays + state.steps
  val labels = days.indices.map { i ->
    today.minusDays((days.size - 1 - i).toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
  }
  val reached = previousDays.count { it >= state.goal }
  val goalText = remember(state.goal, locale) { NumberFormat.getIntegerInstance(locale).format(state.goal) }
  // El primer día no hay cambio de meta que explicar: se explica qué irá apareciendo.
  val phrase = if (previousDays.isEmpty()) stringResource(R.string.first_day_hint) else goalPhrase ?: localGoalPhrase(state.goal, state.previousGoal)
  val description = stringResource(R.string.week_chart_description, reached, state.history.takeLast(6).size, state.goal)

  EmotionBackground(Emotion.HAPPY, intensity = 0.5f) {
    EdgeTitle(stringResource(R.string.week))
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(Modifier.height(40.dp))
      BasicText(
        if (previousDays.isEmpty()) stringResource(R.string.first_day) else stringResource(R.string.days_reached, reached, previousDays.size),
        style = BrasaType.data.copy(color = colors.secondaryText), maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = BrasaType.data.fontSize),
      )
      Canvas(Modifier.fillMaxWidth().weight(1f).semantics { contentDescription = description }) {
        val height = size.height - 18.dp.toPx()
        val ceiling = (days.maxOrNull() ?: 0).coerceAtLeast(state.goal) * 1.12f
        val n = days.size.coerceAtLeast(1)
        val slot = size.width / n
        val width = slot * 0.5f
        val goalY = height - state.goal / ceiling * height + 6.dp.toPx()
        days.forEachIndexed { i, v ->
          if (v < 0) {
            // Día sin registro: solo una marca en la base.
            drawCircle(colors.track, 2.dp.toPx(), Offset(slot * i + slot / 2f, height + 2.dp.toPx()))
            return@forEachIndexed
          }
          val local = ((entrance * 1.3f) - i / n.toFloat() * 0.3f).coerceIn(0f, 1f)
          val h = (v / ceiling * height).coerceAtLeast(width) * local
          val x = slot * i + (slot - width) / 2f
          val isToday = i == days.lastIndex
          val ok = v >= state.goal
          val brush = when {
            isToday -> Brush.verticalGradient(listOf(Tokens.Colors.LightFire, accent, Tokens.Colors.Magenta), startY = height - h, endY = height)
            ok -> Brush.verticalGradient(listOf(accent, lerp(accent, colors.background, 0.25f)), startY = height - h, endY = height)
            else -> Brush.verticalGradient(listOf(lerp(accent, colors.background, 0.55f), lerp(accent, colors.background, 0.7f)), startY = height - h, endY = height)
          }
          if (isToday) {
            val c = Offset(x + width / 2f, height - h * 0.6f)
            drawCircle(Brush.radialGradient(0f to accent.copy(alpha = 0.45f), 1f to Color.Transparent, center = c, radius = width * 2.2f), width * 2.2f, c)
          }
          drawRoundRect(brush, Offset(x, height - h + 6.dp.toPx()), Size(width, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(width / 2f))
        }
        // Línea de meta adaptativa (discontinua, por delante de las barras).
        drawLine(
          if (colors.dark) Tokens.Colors.Gold.copy(alpha = 0.9f) else Tokens.Colors.LightThemeFire, Offset(0f, goalY), Offset(size.width, goalY),
          strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        )
      }
      androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, d ->
          Text(
            d.uppercase(locale), style = BrasaType.data, modifier = Modifier.weight(1f),
            color = if (i == labels.lastIndex) colors.text else colors.secondaryText,
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      Text(stringResource(R.string.goal_value, goalText), style = BrasaType.data, color = if (colors.dark) Tokens.Colors.Gold else Tokens.Colors.LightThemeFire)
      // Abajo el círculo se estrecha: si la frase (local o de la IA) no cabe en 2 líneas, se reduce en lugar de cortarse.
      BasicText(
        phrase, style = BrasaType.voice.copy(fontSize = 13.sp, lineHeight = 15.sp, color = accent), maxLines = 2, overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.sp),
        modifier = Modifier.padding(horizontal = Tokens.Spacing.m),
      )
      Spacer(Modifier.height(22.dp))
    }
  }
}

/** Con menos muestras o menos tramo, Pulso muestra "midiendo" en lugar de una línea sin sentido. */
const val MIN_HEART_RATE_SAMPLES = 6
const val MIN_HEART_RATE_SPAN_MS = 20 * 60_000L
private val AXIS_MARGIN = 12.dp

/** Momento del día a partir de ms (para pruebas). */
fun hourOfMs(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour

/** Estado vacío con el mismo cuidado que el lleno: Brasa tranquila, mensaje y pista de qué hacer. */
@Composable
fun EmptyState(title: String, message: String, track: String, emotion: Emotion) {
  val colors = BrasaTheme.colors
  EmotionBackground(emotion, intensity = 0.35f) {
    EdgeTitle(title)
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
      com.brasa.app.ui.brasa.Brasa(Emotion.CALM, com.brasa.app.ui.brasa.Stage.SPARK, Modifier.fillMaxWidth(0.5f).height(70.dp), animated = false, fixedTime = 1.3f)
      Text(message, style = BrasaType.label, color = colors.text)
      Spacer(Modifier.height(4.dp))
      Text(track, style = BrasaType.voice.copy(fontSize = 12.sp, lineHeight = 14.sp), color = Tokens.palette(Emotion.CALM, colors.dark).accent, maxLines = 2)
    }
  }
}
