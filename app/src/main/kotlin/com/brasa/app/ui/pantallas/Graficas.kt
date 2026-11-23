package com.brasa.app.ui.pantallas

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
import com.brasa.app.EstadoBrasa
import com.brasa.app.R
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.brasa.rememberAnimacionesActivas
import com.brasa.app.ui.componentes.TituloBorde
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
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
private fun rememberEntrada(clave: Any? = Unit, fijo: Boolean = false): Float {
  val animado = rememberAnimacionesActivas() && !fijo
  val p = remember(clave) { Animatable(if (animado) 0f else 1f) }
  LaunchedEffect(clave) { if (animado) p.animateTo(1f, tween(Tokens.Duracion.dibujo, easing = FastOutSlowInEasing)) }
  return p.value
}

// ---------------------------------------------------------------- 1 · Pasos por hora (barras radiales)

@Composable
fun GraficaPasos(estado: EstadoBrasa, horaActual: Int = LocalTime.now().hour, fijo: Boolean = false) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(Emocion.FELIZ, colores.oscuro).acento
  val entrada = rememberEntrada(fijo = fijo)
  val locale = LocalConfiguration.current.locales[0]
  val total = remember(estado.pasos, locale) { NumberFormat.getIntegerInstance(locale).format(estado.pasos) }
  val datos = estado.pasosPorHora
  val max = (datos.maxOrNull() ?: 0).coerceAtLeast(1)
  val pico = datos.indices.maxByOrNull { datos[it] } ?: 0
  val hayDatos = datos.any { it > 0 }
  val desc = stringResource(R.string.grafica_pasos_desc, estado.pasos, pico)

  FondoEmocion(Emocion.FELIZ, intensidad = 0.55f) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = desc }) {
      val c = center
      // Margen seguro: ni la barra más larga ni su brillo llegan al borde (ni al indicador de páginas).
      val rExt = size.minDimension / 2f - Tokens.Espacio.bordeSeguro.toPx()
      val largoMax = rExt * 0.32f
      val rInt = rExt - largoMax
      val grosor = 4.5.dp.toPx()
      for (h in 0 until 24) {
        // Escalonado: cada barra empieza un poco después (barrido horario).
        val local = ((entrada * 1.4f) - h / 24f * 0.4f).coerceIn(0f, 1f)
        val ang = Math.toRadians((-90.0 + h * 15.0))
        val dir = Offset(cos(ang).toFloat(), sin(ang).toFloat())
        val frac = datos[h].toFloat() / max
        val largo = (grosor * 0.6f + largoMax * frac) * local
        val a = c + dir * rInt
        val b = c + dir * (rInt + largo)
        val esAhora = h == horaActual
        val color = when {
          esAhora -> if (colores.oscuro) Tokens.Colores.FuegoClaro else Tokens.Colores.FuegoClaroTema
          h > horaActual -> colores.pista
          else -> lerp(lerp(acento, colores.fondo, 0.6f), acento, frac.coerceIn(0.25f, 1f))
        }
        if (esAhora) {
          drawCircle(
            brush = Brush.radialGradient(0f to color.copy(alpha = 0.6f), 1f to Color.Transparent, center = b, radius = grosor * 2.4f),
            radius = grosor * 2.4f, center = b,
          )
        }
        drawLine(color, a, b, strokeWidth = grosor, cap = StrokeCap.Round)
      }
    }
    // Centro: su ancho sale del radio interior real del anillo de barras (cabe en 192 dp, cuadrado y letra grande).
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      val rInt = (minOf(maxWidth, maxHeight) / 2 - Tokens.Espacio.bordeSeguro) * (1f - 0.32f)
      // La línea de "pico" queda bajo el centro, donde la cuerda del anillo interior es más corta.
      val ancho = rInt * 2 * 0.7f
      Column(Modifier.width(ancho).clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.hora_eje, 0), style = Tipo.dato.copy(fontSize = 10.sp), color = colores.textoSecundario, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        BasicText(
          total, style = Tipo.cifra.copy(color = colores.texto), maxLines = 1,
          autoSize = TextAutoSize.StepBased(minFontSize = 16.sp, maxFontSize = 26.sp),
        )
        BasicText(
          stringResource(R.string.pasos_hoy), style = Tipo.dato.copy(color = colores.textoSecundario), maxLines = 1,
          autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = Tipo.dato.fontSize),
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
          if (hayDatos) stringResource(R.string.pico_a_las, "%02d:00".format(pico)) else stringResource(R.string.sin_pasos_aun),
          style = Tipo.etiqueta.copy(color = acento), maxLines = 1,
          autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 11.sp),
        )
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.hora_eje, 12), style = Tipo.dato.copy(fontSize = 10.sp), color = colores.textoSecundario, maxLines = 1)
      }
    }
  }
}

// ---------------------------------------------------------------- 2 · Pulso del día (línea + área)

@Composable
fun GraficaPulso(estado: EstadoBrasa, fijo: Boolean = false) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(Emocion.INQUIETA, colores.oscuro).acento
  val calma = if (colores.oscuro) Tokens.Colores.Agua else Tokens.Colores.AguaClaroTema
  val entrada = rememberEntrada(fijo = fijo)
  val zona = ZoneId.systemDefault()
  val inicioDia = remember { LocalDate.now().atStartOfDay(zona).toInstant().toEpochMilli() }
  val muestras = estado.pulsos
  val bpm = muestras.map { it.bpm }
  val reposo = if (bpm.size >= 10) bpm.sorted()[(bpm.size * 0.2).toInt()] else bpm.minOrNull()
  val maximo = bpm.maxOrNull()
  val respiro = estado.respiros.lastOrNull()
  val desc = if (maximo != null) stringResource(R.string.grafica_pulso_desc, reposo ?: 0, maximo) else stringResource(R.string.sin_pulso_aun)

  val medidor = rememberTextMeasurer()
  if (muestras.isEmpty()) {
    EstadoVacio(stringResource(R.string.pulso_hoy), stringResource(R.string.sin_pulso_aun), stringResource(R.string.sin_pulso_pista), Emocion.INQUIETA)
    return
  }
  // Pocos datos reales (recién instalada o reloj recién puesto): una línea de 2 puntos no dice nada.
  if (muestras.size < MIN_MUESTRAS_PULSO || muestras.last().ms - muestras.first().ms < MIN_TRAMO_PULSO_MS) {
    EstadoVacio(
      stringResource(R.string.pulso_hoy), stringResource(R.string.pulso_midiendo),
      stringResource(R.string.pulso_midiendo_pista, muestras.last().bpm), Emocion.INQUIETA,
    )
    return
  }
  FondoEmocion(Emocion.INQUIETA, intensidad = 0.45f) {
    TituloBorde(stringResource(R.string.pulso_hoy))
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(Modifier.height(40.dp))
      Text(
        if (maximo != null) stringResource(R.string.reposo_max, reposo ?: 0, maximo) else stringResource(R.string.sin_pulso_aun),
        style = Tipo.dato, color = colores.textoSecundario, maxLines = 1,
      )
      Spacer(Modifier.height(6.dp))
      Canvas(Modifier.fillMaxWidth().weight(1f).semantics { contentDescription = desc }) {
        // Margen lateral: abajo el círculo se estrecha; el eje y sus etiquetas quedan dentro.
        val lado = MARGEN_EJE.toPx()
        val area = Rect(lado, 14.dp.toPx(), size.width - lado, size.height - 16.dp.toPx())
        // Línea base y guía de reposo + 20 (umbral de tensión), discontinua.
        drawLine(colores.pista, Offset(area.left, area.bottom), Offset(area.right, area.bottom), strokeWidth = 1.dp.toPx())
        if (muestras.size < 2) return@Canvas
        val minY = (bpm.min() - 8).coerceAtLeast(40)
        val maxY = bpm.max() + 8
        fun x(ms: Long) = area.left + ((ms - inicioDia) / 86_400_000f).coerceIn(0f, 1f) * area.width
        fun y(v: Int) = area.bottom - (v - minY).toFloat() / (maxY - minY) * area.height
        if (reposo != null) {
          val yu = y(reposo + com.brasa.app.domain.Reglas.MARGEN_TENSION_LPM)
          drawLine(
            colores.textoSecundario.copy(alpha = 0.45f), Offset(area.left, yu), Offset(area.right, yu),
            strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
          )
        }
        // Máximo ~96 puntos: suficiente para un reloj y barato de dibujar.
        val paso = (muestras.size / 96).coerceAtLeast(1)
        val pts = muestras.filterIndexed { i, _ -> i % paso == 0 || i == muestras.lastIndex }.map { Offset(x(it.ms), y(it.bpm)) }
        val linea = Path().apply { pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        val relleno = Path().apply {
          addPath(linea); lineTo(pts.last().x, area.bottom); lineTo(pts.first().x, area.bottom); close()
        }
        // Se revela de izquierda a derecha al entrar.
        clipRect(right = area.left + (area.width + 20f) * entrada) {
          drawPath(relleno, Brush.verticalGradient(listOf(acento.copy(alpha = 0.5f), acento.copy(alpha = 0f)), startY = area.top, endY = area.bottom))
          drawPath(linea, acento, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
          val iMax = muestras.indexOfFirst { it.bpm == maximo }
          if (iMax >= 0) {
            val p = Offset(x(muestras[iMax].ms), y(muestras[iMax].bpm))
            drawCircle(acento, 4.dp.toPx(), p)
            drawCircle(colores.fondo, 1.8.dp.toPx(), p)
            // Cifra del pico encima del punto.
            val etiqueta = medidor.measure("${muestras[iMax].bpm}", Tipo.dato.copy(color = acento))
            drawText(etiqueta, topLeft = Offset(p.x - etiqueta.size.width / 2f, p.y - etiqueta.size.height - 5.dp.toPx()))
          }
          if (respiro != null) {
            val rx = x(respiro.ms)
            drawLine(calma.copy(alpha = 0.7f), Offset(rx, area.top), Offset(rx, area.bottom), strokeWidth = 1.5.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 5f)))
            val cerca = muestras.minByOrNull { kotlin.math.abs(it.ms - respiro.ms) }
            if (cerca != null) {
              val p = Offset(rx, y(cerca.bpm))
              drawCircle(calma, 5.dp.toPx(), p)
              drawCircle(colores.fondo, 2.dp.toPx(), p)
            }
          }
        }
        // Eje: 0 h · 12 h · 24 h (texto en Compose abajo).
      }
      Box(Modifier.fillMaxWidth().padding(horizontal = MARGEN_EJE)) {
        Text(stringResource(R.string.hora_eje, 0), style = Tipo.dato, color = colores.textoSecundario, modifier = Modifier.align(Alignment.CenterStart))
        Text(stringResource(R.string.hora_eje, 12), style = Tipo.dato, color = colores.textoSecundario, modifier = Modifier.align(Alignment.Center))
        Text(stringResource(R.string.hora_eje, 24), style = Tipo.dato, color = colores.textoSecundario, modifier = Modifier.align(Alignment.CenterEnd))
      }
      Spacer(Modifier.height(6.dp))
      // Chip del evento de respiración (mismo color de "calma" que el marcador).
      val bajada = respiro?.let { r -> if (r.antes != null && r.despues != null) r.despues - r.antes else null }
      // Solo se destaca si el pulso bajó (no se presenta una subida como logro).
      if (bajada != null && bajada < 0) {
        Box(
          Modifier.padding(horizontal = 10.dp).height(30.dp).clip(CircleShape).background(calma.copy(alpha = if (colores.oscuro) 0.16f else 0.12f))
            .padding(horizontal = 12.dp),
          contentAlignment = Alignment.Center,
        ) {
          // Se ajusta al ancho disponible (fuente grande, pantalla pequeña) en lugar de cortarse.
          BasicText(
            stringResource(R.string.tras_respirar, "−${-bajada}"), style = Tipo.etiqueta.copy(color = calma), maxLines = 1,
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
fun fraseMetaLocal(meta: Int, anterior: Int): String = when {
  meta > anterior -> stringResource(R.string.meta_sube)
  meta < anterior -> stringResource(R.string.meta_baja)
  else -> stringResource(R.string.meta_igual)
}

@Composable
fun GraficaSemana(estado: EstadoBrasa, fraseMeta: String? = null, fijo: Boolean = false, hoy: LocalDate = LocalDate.now()) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(Emocion.FELIZ, colores.oscuro).acento
  val entrada = rememberEntrada(fijo = fijo)
  val locale = LocalConfiguration.current.locales[0]
  // Siempre 7 huecos (los días sin registro quedan vacíos): la barra de hoy nunca ocupa todo el ancho.
  val previos = estado.historial.takeLast(6)
  val dias = List(6 - previos.size) { -1 } + previos + estado.pasos
  val etiquetas = dias.indices.map { i ->
    hoy.minusDays((dias.size - 1 - i).toLong()).dayOfWeek.getDisplayName(TextStyle.NARROW, locale)
  }
  val cumplidos = previos.count { it >= estado.meta }
  val metaTxt = remember(estado.meta, locale) { NumberFormat.getIntegerInstance(locale).format(estado.meta) }
  // El primer día no hay cambio de meta que explicar: se explica qué irá apareciendo.
  val frase = if (previos.isEmpty()) stringResource(R.string.primer_dia_pista) else fraseMeta ?: fraseMetaLocal(estado.meta, estado.metaAnterior)
  val desc = stringResource(R.string.grafica_semana_desc, cumplidos, estado.historial.takeLast(6).size, estado.meta)

  FondoEmocion(Emocion.FELIZ, intensidad = 0.5f) {
    TituloBorde(stringResource(R.string.semana))
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(Modifier.height(40.dp))
      BasicText(
        if (previos.isEmpty()) stringResource(R.string.primer_dia) else stringResource(R.string.dias_cumplidos, cumplidos, previos.size),
        style = Tipo.dato.copy(color = colores.textoSecundario), maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = Tipo.dato.fontSize),
      )
      Canvas(Modifier.fillMaxWidth().weight(1f).semantics { contentDescription = desc }) {
        val alto = size.height - 18.dp.toPx()
        val techo = (dias.maxOrNull() ?: 0).coerceAtLeast(estado.meta) * 1.12f
        val n = dias.size.coerceAtLeast(1)
        val hueco = size.width / n
        val ancho = hueco * 0.5f
        val yMeta = alto - estado.meta / techo * alto + 6.dp.toPx()
        dias.forEachIndexed { i, v ->
          if (v < 0) {
            // Día sin registro: solo una marca en la base.
            drawCircle(colores.pista, 2.dp.toPx(), Offset(hueco * i + hueco / 2f, alto + 2.dp.toPx()))
            return@forEachIndexed
          }
          val local = ((entrada * 1.3f) - i / n.toFloat() * 0.3f).coerceIn(0f, 1f)
          val h = (v / techo * alto).coerceAtLeast(ancho) * local
          val x = hueco * i + (hueco - ancho) / 2f
          val esHoy = i == dias.lastIndex
          val ok = v >= estado.meta
          val brush = when {
            esHoy -> Brush.verticalGradient(listOf(Tokens.Colores.FuegoClaro, acento, Tokens.Colores.Magenta), startY = alto - h, endY = alto)
            ok -> Brush.verticalGradient(listOf(acento, lerp(acento, colores.fondo, 0.25f)), startY = alto - h, endY = alto)
            else -> Brush.verticalGradient(listOf(lerp(acento, colores.fondo, 0.55f), lerp(acento, colores.fondo, 0.7f)), startY = alto - h, endY = alto)
          }
          if (esHoy) {
            val c = Offset(x + ancho / 2f, alto - h * 0.6f)
            drawCircle(Brush.radialGradient(0f to acento.copy(alpha = 0.45f), 1f to Color.Transparent, center = c, radius = ancho * 2.2f), ancho * 2.2f, c)
          }
          drawRoundRect(brush, Offset(x, alto - h + 6.dp.toPx()), Size(ancho, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(ancho / 2f))
        }
        // Línea de meta adaptativa (discontinua, por delante de las barras).
        drawLine(
          if (colores.oscuro) Tokens.Colores.Dorado.copy(alpha = 0.9f) else Tokens.Colores.FuegoClaroTema, Offset(0f, yMeta), Offset(size.width, yMeta),
          strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        )
      }
      androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth()) {
        etiquetas.forEachIndexed { i, d ->
          Text(
            d.uppercase(locale), style = Tipo.dato, modifier = Modifier.weight(1f),
            color = if (i == etiquetas.lastIndex) colores.texto else colores.textoSecundario,
          )
        }
      }
      Spacer(Modifier.height(4.dp))
      Text(stringResource(R.string.meta_valor, metaTxt), style = Tipo.dato, color = if (colores.oscuro) Tokens.Colores.Dorado else Tokens.Colores.FuegoClaroTema)
      Text(frase, style = Tipo.voz.copy(fontSize = 13.sp, lineHeight = 15.sp), color = acento, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
      Spacer(Modifier.height(22.dp))
    }
  }
}

/** Con menos muestras o menos tramo, Pulso muestra "midiendo" en lugar de una línea sin sentido. */
const val MIN_MUESTRAS_PULSO = 6
const val MIN_TRAMO_PULSO_MS = 20 * 60_000L
private val MARGEN_EJE = 12.dp

/** Momento del día a partir de ms (para pruebas). */
fun horaDeMs(ms: Long): Int = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).hour

/** Estado vacío con el mismo cuidado que el lleno: Brasa tranquila, mensaje y pista de qué hacer. */
@Composable
fun EstadoVacio(titulo: String, mensaje: String, pista: String, emocion: Emocion) {
  val colores = BrasaTema.colores
  FondoEmocion(emocion, intensidad = 0.35f) {
    TituloBorde(titulo)
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
      com.brasa.app.ui.brasa.Brasa(Emocion.TRANQUILA, com.brasa.app.ui.brasa.Etapa.CHISPA, Modifier.fillMaxWidth(0.5f).height(70.dp), animado = false, tiempoFijo = 1.3f)
      Text(mensaje, style = Tipo.etiqueta, color = colores.texto)
      Spacer(Modifier.height(4.dp))
      Text(pista, style = Tipo.voz.copy(fontSize = 12.sp, lineHeight = 14.sp), color = Tokens.paleta(Emocion.TRANQUILA, colores.oscuro).acento, maxLines = 2)
    }
  }
}
