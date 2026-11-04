package com.brasa.app.ui.brasa

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import com.brasa.app.ui.theme.Tokens
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Dibuja a Brasa por capas, de atrás hacia delante:
 * brillo → sombra → llamas laterales → piernas → cuerpo → brazos → cara → lágrima/sudor → chispas/corona/órbita.
 * Sin blur: todos los brillos son degradados radiales (compatibles con API 30).
 *
 * @param t tiempo en segundos (anima respiración, flotación, llama, brazos).
 * @param parpadeo 0 = ojos abiertos, 1 = cerrados.
 * @param rebote escala de rebote (transiciones y toques).
 * @param risa 0..1 reacción al toque (ojos felices, boca abierta).
 */
fun DrawScope.dibujarBrasa(
  pose: Pose,
  forma: Forma,
  t: Float,
  parpadeo: Float,
  rebote: Float,
  risa: Float,
  oscuro: Boolean,
) {
  val s = min(size.width, size.height)
  val u = s / 100f
  val r = 21f * u * pose.escala * forma.escala
  val ext = forma.extremidades
  val largoPierna = 0.46f * r * ext

  // Centro: deja sitio abajo para piernas y sombra.
  val cx0 = size.width / 2f
  // Con corona, Brasa baja un poco para no invadir el texto curvo superior.
  val cy0 = size.height / 2f - 2f * u + 9f * u * forma.corona
  val flota = sin(t * pose.flotaVel) * pose.flotaAmp * u
  val salto = -abs(sin(t * 3.2f)) * pose.salto * 7f * u
  val temblor = (sin(t * 47f) * 0.7f + sin(t * 31f) * 0.4f) * pose.temblor * u
  val cx = cx0 + temblor
  val cy = cy0 + flota + salto
  val resp = sin(t * 2.1f)
  val sx = rebote * (1f - 0.018f * resp)
  val sy = rebote * (1f + 0.03f * resp)
  fun p(x: Float, y: Float) = Offset(cx + x * sx, cy + y * sy)

  // Expresión final (la risa del toque se impone a la emoción).
  val ojoFeliz = maxOf(pose.ojoFeliz, risa)
  val bocaAbierta = maxOf(pose.bocaAbierta, risa)
  val boca = pose.boca + (1f - pose.boca) * risa
  val bocaTemblor = pose.bocaTemblor * (1f - risa)

  val alturaPunta = r * 1.15f * forma.punta * (1f + 0.07f * sin(t * 7.3f) * pose.avivar)
  val vaiven = r * (0.24f + 0.12f * sin(t * 3.1f) * pose.avivar + 0.05f * sin(t * 8.7f) * pose.avivar)

  // 1 · Brillo (luz propia en oscuro; halo tenue en claro).
  val alfaBrillo = pose.brillo * if (oscuro) 0.55f else 0.22f
  drawCircle(
    brush = Brush.radialGradient(
      0f to pose.base.copy(alpha = alfaBrillo),
      0.5f to pose.base.copy(alpha = alfaBrillo * 0.35f),
      1f to Color.Transparent,
      center = Offset(cx, cy - 0.3f * r),
      radius = r * 2.9f,
    ),
    radius = r * 2.9f,
    center = Offset(cx, cy - 0.3f * r),
  )

  // 2 · Sombra elíptica en el suelo (se encoge al subir).
  val suelo = cy0 + r + largoPierna + 6f * u
  val altura = ((cy0 - cy) / (8f * u)).coerceIn(-0.4f, 1f)
  val anchoSombra = r * 1.35f * (1f - 0.35f * altura)
  val colorSombra = if (oscuro) Color.Black.copy(alpha = 0.5f) else pose.borde.copy(alpha = 0.28f)
  drawOval(
    brush = Brush.radialGradient(
      0f to colorSombra, 1f to Color.Transparent,
      center = Offset(cx0, suelo), radius = anchoSombra,
    ),
    topLeft = Offset(cx0 - anchoSombra, suelo - 0.14f * r),
    size = Size(anchoSombra * 2f, 0.28f * r),
  )

  // 3 · Llamas laterales (Fogata y Leyenda).
  if (forma.laterales > 0.01f) {
    for (lado in intArrayOf(-1, 1)) {
      val ang = lado * (16f + 5f * sin(t * 2.4f + lado))
      val path = rutaLlama(
        origen = p(lado * 1.02f * r, 0.98f * r), angulo = ang,
        radio = 0.34f * r * forma.laterales, alto = 1.35f * r * forma.laterales * (1f + 0.08f * sin(t * 5.3f + lado)),
        vaiven = lado * 0.12f * r * sin(t * 4.1f + lado * 1.3f),
      )
      drawPath(
        path,
        brush = Brush.verticalGradient(
          0f to pose.nucleo, 0.6f to pose.base, 1f to pose.borde,
          startY = cy - 1.4f * r, endY = cy + 0.5f * r,
        ),
      )
    }
  }

  // 4 · Piernas (detrás del cuerpo): cápsulas cortas con pie redondeado.
  val colorPierna = lerp(pose.base, pose.borde, 0.45f)
  val colorPie = lerp(pose.base, pose.borde, 0.6f)
  for (lado in intArrayOf(-1, 1)) {
    val patada = sin(t * pose.flotaVel * 1.4f + lado) * 7f + pose.salto * 18f
    val a = Math.toRadians((lado * (6f + patada * lado)).toDouble()).toFloat()
    val cadera = p(lado * 0.34f * r, 0.8f * r)
    val largo = largoPierna * (1f - 0.18f * pose.salto * abs(sin(t * 3.2f)))
    val pie = Offset(cadera.x + sin(a) * largo, cadera.y + cos(a) * largo)
    drawLine(colorPierna, cadera, pie, strokeWidth = 0.24f * r * ext, cap = StrokeCap.Round)
    val anchoPie = 0.36f * r * ext
    drawOval(
      colorPie,
      topLeft = Offset(pie.x - anchoPie / 2f + lado * 0.07f * r, pie.y - 0.08f * r),
      size = Size(anchoPie, 0.2f * r * ext),
    )
  }

  // 5 · Cuerpo: gota de fuego con volumen + dos lenguas secundarias unidas a la silueta
  // (tres puntas = se lee como fuego, no como gota de agua).
  val lenguas = forma.punta.coerceAtMost(1f)
  val cuerpo = Path().apply {
    op(
      rutaCuerpo(::p, r, alturaPunta, vaiven),
      Path().apply {
        op(
          rutaLlama(
            p(-0.5f * r, -0.42f * r), angulo = -24f + 5f * sin(t * 3.7f) * pose.avivar,
            radio = 0.26f * r, alto = 1.05f * r * lenguas * (1f + 0.08f * sin(t * 6.1f) * pose.avivar),
            vaiven = -0.08f * r * sin(t * 4.3f),
          ),
          rutaLlama(
            p(0.56f * r, -0.4f * r), angulo = 26f + 5f * sin(t * 4.1f + 1f) * pose.avivar,
            radio = 0.2f * r, alto = 0.78f * r * lenguas * (1f + 0.1f * sin(t * 7.7f + 2f) * pose.avivar),
            vaiven = 0.06f * r * sin(t * 5.1f),
          ),
          PathOperation.Union,
        )
      },
      PathOperation.Union,
    )
  }
  drawPath(
    cuerpo,
    brush = Brush.radialGradient(
      0f to pose.nucleo, 0.42f to pose.base, 1f to pose.borde,
      center = p(-0.32f * r, -0.3f * r), radius = r * 1.8f * rebote,
    ),
  )
  // Punta encendida: llama interior más clara que se desvanece antes de la cara.
  val puntaY = -(r + alturaPunta)
  clipPath(cuerpo) {
    drawPath(
      rutaCuerpo(::p, r * 0.62f, alturaPunta * 1.05f, vaiven * 1.1f, desplazY = -0.32f * r),
      brush = Brush.verticalGradient(
        0f to pose.nucleo.copy(alpha = 0.7f), 0.5f to pose.nucleo.copy(alpha = 0.16f), 1f to Color.Transparent,
        startY = p(0f, puntaY).y, endY = p(0f, -0.25f * r).y,
      ),
    )
  }
  // Borde: más marcado en claro (sombra en vez de brillo).
  drawPath(cuerpo, color = pose.borde.copy(alpha = if (oscuro) 0.55f else 0.9f), style = Stroke(width = 0.045f * r))
  // Brillo especular arriba a la izquierda.
  val esp = p(-0.5f * r, -0.48f * r)
  rotate(-32f, pivot = esp) {
    drawOval(
      brush = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.5f), 1f to Color.White.copy(alpha = 0f),
        center = esp, radius = 0.26f * r,
      ),
      topLeft = Offset(esp.x - 0.13f * r, esp.y - 0.25f * r),
      size = Size(0.26f * r, 0.5f * r),
    )
  }
  drawCircle(Color.White.copy(alpha = 0.45f), radius = 0.04f * r, center = p(-0.3f * r, -0.72f * r))

  // 6 · Brazos (delante): "manguera" curva con mano redonda.
  val colorBrazo = lerp(pose.base, pose.borde, 0.12f)
  val colorMano = lerp(pose.base, pose.nucleo, 0.22f)
  for (lado in intArrayOf(-1, 1)) {
    var ang = if (lado < 0) pose.brazoIzq else pose.brazoDer
    ang += sin(t * pose.flotaVel * 1.2f + lado * 0.8f) * pose.balanceo
    if (lado > 0) ang += pose.saludo * sin(t * 9f) * 22f
    ang += sin(t * 40f + lado) * 4f * pose.temblor
    val a = Math.toRadians(ang.toDouble()).toFloat()
    val hombro = p(lado * 0.86f * r, 0.32f * r)
    val largo = 0.64f * r * ext
    val dx = lado * sin(a)
    val dy = cos(a)
    val mano = Offset(hombro.x + dx * largo, hombro.y + dy * largo)
    val control = Offset(
      (hombro.x + mano.x) / 2f + (-dy) * lado * 0.14f * r,
      (hombro.y + mano.y) / 2f + dx * lado * 0.14f * r,
    )
    val brazo = Path().apply { moveTo(hombro.x, hombro.y); quadraticTo(control.x, control.y, mano.x, mano.y) }
    drawPath(brazo, pose.borde.copy(alpha = 0.6f), style = Stroke(0.27f * r * ext, cap = StrokeCap.Round))
    drawPath(brazo, colorBrazo, style = Stroke(0.2f * r * ext, cap = StrokeCap.Round))
    drawCircle(pose.borde.copy(alpha = 0.6f), radius = 0.175f * r * ext, center = mano)
    drawCircle(colorMano, radius = 0.145f * r * ext, center = mano)
  }

  // 7 · Cara.
  dibujarCara(::p, r, pose, t, parpadeo, ojoFeliz, bocaAbierta, boca, bocaTemblor)

  // 8 · Lágrima (apagada) y gota de sudor (inquieta).
  if (pose.lagrima > 0.01f) {
    val f = (t * 0.45f) % 1f
    dibujarGota(p(-0.4f * r, 0.16f * r + f * 0.5f * r), 0.07f * r, pose.lagrima * (1f - f))
  }
  if (pose.sudor > 0.01f) {
    val f = (t * 0.55f) % 1f
    dibujarGota(p(0.82f * r, -0.72f * r + f * 0.35f * r), 0.09f * r, pose.sudor * (1f - f * 0.6f))
  }

  // 9 · Chispas (eufórica), corona y órbita (Leyenda).
  if (pose.chispas > 0.01f) {
    for (i in 0 until 6) {
      val ang = (i * 60f + t * 22f) * (PI.toFloat() / 180f)
      val rad = 1.9f * r + sin(t * 3f + i) * 0.12f * r
      val centro = Offset(cx + cos(ang) * rad, cy - 0.35f * r + sin(ang) * rad * 0.9f)
      val tam = 0.13f * r * (0.6f + 0.4f * sin(t * 5f + i * 1.7f))
      dibujarEstrella(centro, tam, if (i % 2 == 0) Tokens.Colores.Dorado else Tokens.Colores.FuegoClaro, pose.chispas)
    }
  }
  if (forma.corona > 0.01f) {
    dibujarCorona(p(vaiven * 0.5f, puntaY - 0.34f * r + sin(t * 2f) * 0.04f * r), r * 1.35f, forma.corona)
  }
  if (forma.orbita > 0.01f) {
    for (i in 0 until 3) {
      val ang = t * 1.4f + i * 2.094f
      val centro = Offset(cx + cos(ang) * 2.05f * r, cy - 0.1f * r + sin(ang) * 0.55f * r)
      val tam = 0.07f * r * (1.1f + 0.4f * sin(ang))
      drawCircle(
        brush = Brush.radialGradient(
          0f to Tokens.Colores.FuegoClaro, 0.4f to Tokens.Colores.Dorado.copy(alpha = 0.9f), 1f to Color.Transparent,
          center = centro, radius = tam * 2.6f,
        ),
        radius = tam * 2.6f * forma.orbita, center = centro,
      )
    }
  }
}

/** Gota de fuego del cuerpo. Origen en el centro del bulbo; punta hacia arriba con vaivén. */
private fun rutaCuerpo(
  p: (Float, Float) -> Offset,
  r: Float,
  alturaPunta: Float,
  vaiven: Float,
  desplazY: Float = 0f,
): Path = Path().apply {
  val tipY = -r - alturaPunta
  val inicio = p(vaiven, tipY + desplazY)
  moveTo(inicio.x, inicio.y)
  val c1 = p(vaiven * 0.6f + 0.2f * r, -r - alturaPunta * 0.55f + desplazY)
  val c2 = p(1.04f * r, -0.8f * r + desplazY)
  val e1 = p(r, 0.05f * r + desplazY)
  cubicTo(c1.x, c1.y, c2.x, c2.y, e1.x, e1.y)
  val c3 = p(r, 0.6f * r + desplazY); val c4 = p(0.56f * r, r + desplazY); val e2 = p(0f, r + desplazY)
  cubicTo(c3.x, c3.y, c4.x, c4.y, e2.x, e2.y)
  val c5 = p(-0.56f * r, r + desplazY); val c6 = p(-r, 0.6f * r + desplazY); val e3 = p(-r, 0.05f * r + desplazY)
  cubicTo(c5.x, c5.y, c6.x, c6.y, e3.x, e3.y)
  val c7 = p(-1.02f * r, -0.85f * r + desplazY); val c8 = p(vaiven * 0.4f - 0.5f * r, -r - alturaPunta * 0.4f + desplazY)
  val e4 = p(vaiven, tipY + desplazY)
  cubicTo(c7.x, c7.y, c8.x, c8.y, e4.x, e4.y)
  close()
}

/** Lengua de fuego genérica (llamas laterales, corona). Base en [origen], crece en dirección [angulo]. */
private fun rutaLlama(origen: Offset, angulo: Float, radio: Float, alto: Float, vaiven: Float): Path {
  val a = Math.toRadians(angulo.toDouble())
  val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
  // Local: base en (0,0), punta en (vaiven, -alto). Rota y traslada.
  fun m(x: Float, y: Float) = Offset(origen.x + x * ca - y * sa, origen.y + x * sa + y * ca)
  val r = radio
  return Path().apply {
    val tip = m(vaiven, -alto); moveTo(tip.x, tip.y)
    var c1 = m(vaiven * 0.5f + r * 0.3f, -alto * 0.6f); var c2 = m(r * 1.05f, -r * 1.7f); var e = m(r, -r)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    c1 = m(r, -r * 0.4f); c2 = m(r * 0.55f, 0f); e = m(0f, 0f)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    c1 = m(-r * 0.55f, 0f); c2 = m(-r, -r * 0.4f); e = m(-r, -r)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    c1 = m(-r * 1.05f, -r * 1.7f); c2 = m(vaiven * 0.5f - r * 0.5f, -alto * 0.55f); e = m(vaiven, -alto)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    close()
  }
}

private fun DrawScope.dibujarCara(
  p: (Float, Float) -> Offset,
  r: Float,
  pose: Pose,
  t: Float,
  parpadeo: Float,
  ojoFeliz: Float,
  bocaAbierta: Float,
  boca: Float,
  bocaTemblor: Float,
) {
  val tinta = Tokens.Colores.Pupila
  val rx = 0.15f * r
  val ry = (0.2f * r * pose.ojoAbierto * (1f - parpadeo)).coerceAtLeast(0.014f * r)

  for (lado in intArrayOf(-1, 1)) {
    val c = p(lado * 0.39f * r, -0.02f * r)

    // Ojo normal (con párpado caído si está apagada).
    val alfaOjo = 1f - ojoFeliz
    if (alfaOjo > 0.01f) {
      var ojo = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - rx, c.y - ry, c.x + rx, c.y + ry)) }
      if (pose.ojoCaido > 0.01f) {
        val exterior = lado * rx * 1.6f
        val interior = -lado * rx * 1.6f
        val yExt = c.y - ry + pose.ojoCaido * ry * 1.15f
        val yInt = c.y - ry + pose.ojoCaido * ry * 0.5f
        val parpado = Path().apply {
          moveTo(c.x + exterior, yExt); lineTo(c.x + interior, yInt)
          lineTo(c.x + interior, c.y - ry * 3f); lineTo(c.x + exterior, c.y - ry * 3f); close()
        }
        ojo = Path().apply { op(ojo, parpado, PathOperation.Difference) }
        drawLine(
          tinta.copy(alpha = alfaOjo * pose.ojoCaido),
          Offset(c.x + lado * rx * 1.05f, yExt + lado * 0f), Offset(c.x - lado * rx * 1.05f, yInt),
          strokeWidth = 0.045f * r, cap = StrokeCap.Round,
        )
      }
      drawPath(ojo, tinta.copy(alpha = alfaOjo))
      val alfaReflejo = alfaOjo * (1f - pose.ojoCaido * 1.3f).coerceIn(0f, 1f) * (1f - parpadeo)
      if (alfaReflejo > 0.01f) {
        drawCircle(Color.White.copy(alpha = alfaReflejo), radius = 0.048f * r * pose.ojoAbierto.coerceAtMost(1.15f),
          center = Offset(c.x + 0.035f * r, c.y - 0.06f * r * pose.ojoAbierto))
        drawCircle(Color.White.copy(alpha = alfaReflejo * 0.7f), radius = 0.022f * r,
          center = Offset(c.x - 0.04f * r, c.y + 0.05f * r * pose.ojoAbierto))
      }
    }
    // Ojos felices "^ ^".
    if (ojoFeliz > 0.01f) {
      val arco = Path().apply {
        moveTo(c.x - rx * 1.1f, c.y + 0.04f * r)
        quadraticTo(c.x, c.y - 0.22f * r, c.x + rx * 1.1f, c.y + 0.04f * r)
      }
      drawPath(arco, tinta.copy(alpha = ojoFeliz), style = Stroke(0.065f * r, cap = StrokeCap.Round))
    }
    // Cejas de preocupación (extremo interior hacia arriba).
    if (pose.cejaVisible > 0.01f) {
      val cc = Offset(c.x, c.y - 0.29f * r - 0.05f * r * pose.ojoAbierto)
      drawLine(
        tinta.copy(alpha = pose.cejaVisible),
        Offset(cc.x - lado * 0.12f * r, cc.y - pose.ceja * 0.06f * r),
        Offset(cc.x + lado * 0.12f * r, cc.y + pose.ceja * 0.04f * r),
        strokeWidth = 0.05f * r, cap = StrokeCap.Round,
      )
    }
    // Mejillas.
    if (pose.mejillas > 0.01f) {
      val m = p(lado * 0.64f * r, 0.24f * r)
      drawOval(
        brush = Brush.radialGradient(
          0f to Tokens.Colores.Mejilla.copy(alpha = 0.65f * pose.mejillas), 1f to Color.Transparent,
          center = m, radius = 0.13f * r,
        ),
        topLeft = Offset(m.x - 0.13f * r, m.y - 0.075f * r), size = Size(0.26f * r, 0.15f * r),
      )
    }
  }

  // Boca.
  val mb = p(0f, 0.3f * r)
  val ancho = 0.21f * r
  val alfaLinea = (1f - bocaAbierta) * (1f - bocaTemblor)
  if (alfaLinea > 0.01f) {
    val sonrisa = Path().apply {
      moveTo(mb.x - ancho, mb.y); quadraticTo(mb.x, mb.y + boca * 0.2f * r, mb.x + ancho, mb.y)
    }
    drawPath(sonrisa, tinta.copy(alpha = alfaLinea), style = Stroke(0.06f * r, cap = StrokeCap.Round))
  }
  if (bocaAbierta > 0.01f) {
    val fondo = mb.y + 0.05f * r + 0.36f * r * bocaAbierta
    val abierta = Path().apply {
      moveTo(mb.x - ancho * 1.1f, mb.y - 0.02f * r)
      quadraticTo(mb.x, mb.y + 0.03f * r, mb.x + ancho * 1.1f, mb.y - 0.02f * r)
      quadraticTo(mb.x, fondo, mb.x - ancho * 1.1f, mb.y - 0.02f * r)
      close()
    }
    drawPath(abierta, Tokens.Colores.Boca.copy(alpha = bocaAbierta))
    clipPath(abierta) {
      drawCircle(Tokens.Colores.Lengua.copy(alpha = bocaAbierta), radius = 0.12f * r,
        center = Offset(mb.x + 0.03f * r, fondo - 0.02f * r))
    }
  }
  if (bocaTemblor > 0.01f) {
    val onda = Path()
    val pasos = 16
    for (i in 0..pasos) {
      val f = i / pasos.toFloat()
      val x = mb.x - ancho + f * ancho * 2f
      val y = mb.y + 0.02f * r + sin(f * PI.toFloat() * 5f + t * 12f) * 0.035f * r
      if (i == 0) onda.moveTo(x, y) else onda.lineTo(x, y)
    }
    drawPath(onda, tinta.copy(alpha = bocaTemblor), style = Stroke(0.05f * r, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

/** Gota de agua (lágrima o sudor) con reflejo. */
private fun DrawScope.dibujarGota(c: Offset, radio: Float, alfa: Float) {
  if (alfa <= 0.01f) return
  val gota = Path().apply {
    moveTo(c.x, c.y - radio * 2.3f)
    cubicTo(c.x + radio * 0.4f, c.y - radio * 1.4f, c.x + radio, c.y - radio * 0.6f, c.x + radio, c.y)
    cubicTo(c.x + radio, c.y + radio * 0.6f, c.x + radio * 0.55f, c.y + radio, c.x, c.y + radio)
    cubicTo(c.x - radio * 0.55f, c.y + radio, c.x - radio, c.y + radio * 0.6f, c.x - radio, c.y)
    cubicTo(c.x - radio, c.y - radio * 0.6f, c.x - radio * 0.4f, c.y - radio * 1.4f, c.x, c.y - radio * 2.3f)
    close()
  }
  drawPath(gota, Tokens.Colores.Agua.copy(alpha = alfa))
  drawCircle(Color.White.copy(alpha = alfa * 0.8f), radius = radio * 0.3f, center = Offset(c.x - radio * 0.35f, c.y - radio * 0.1f))
}

/** Estrella de cuatro puntas (chispa de celebración). */
private fun DrawScope.dibujarEstrella(c: Offset, tam: Float, color: Color, alfa: Float) {
  val e = Path().apply {
    moveTo(c.x, c.y - tam)
    quadraticTo(c.x, c.y, c.x + tam, c.y)
    quadraticTo(c.x, c.y, c.x, c.y + tam)
    quadraticTo(c.x, c.y, c.x - tam, c.y)
    quadraticTo(c.x, c.y, c.x, c.y - tam)
    close()
  }
  drawCircle(
    brush = Brush.radialGradient(0f to color.copy(alpha = 0.45f * alfa), 1f to Color.Transparent, center = c, radius = tam * 1.6f),
    radius = tam * 1.6f, center = c,
  )
  drawPath(e, color.copy(alpha = alfa))
}

/** Corona de fuego dorada (Leyenda): tres llamas y una banda. */
private fun DrawScope.dibujarCorona(c: Offset, r: Float, alfa: Float) {
  val oro = Brush.verticalGradient(
    0f to Tokens.Colores.FuegoClaro, 1f to Tokens.Colores.Dorado,
    startY = c.y - 0.5f * r, endY = c.y + 0.05f * r,
  )
  drawCircle(
    brush = Brush.radialGradient(0f to Tokens.Colores.Dorado.copy(alpha = 0.5f * alfa), 1f to Color.Transparent, center = c, radius = 0.7f * r),
    radius = 0.7f * r, center = c,
  )
  val puntas = listOf(Triple(-0.2f, -18f, 0.28f), Triple(0.2f, 18f, 0.28f), Triple(0f, 0f, 0.4f))
  for ((dx, ang, alto) in puntas) {
    drawPath(
      rutaLlama(Offset(c.x + dx * r, c.y), ang, radio = 0.085f * r, alto = alto * r, vaiven = 0f),
      brush = oro, alpha = alfa,
    )
  }
  drawRoundRect(
    brush = oro, alpha = alfa,
    topLeft = Offset(c.x - 0.3f * r, c.y - 0.06f * r), size = Size(0.6f * r, 0.11f * r),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.055f * r),
  )
}
