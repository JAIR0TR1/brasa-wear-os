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
fun DrawScope.drawBrasa(
  pose: Pose,
  shape: BrasaShape,
  t: Float,
  blink: Float,
  bounce: Float,
  laugh: Float,
  dark: Boolean,
) {
  val s = min(size.width, size.height)
  val u = s / 100f
  val r = 21f * u * pose.scale * shape.scale
  val ext = shape.limbs
  val legLength = 0.46f * r * ext

  // Centro: deja sitio abajo para piernas y sombra.
  val cx0 = size.width / 2f
  // Con corona, Brasa baja un poco para no invadir el texto curvo superior.
  val cy0 = size.height / 2f - 2f * u + 9f * u * shape.crown
  val hover = sin(t * pose.hoverSpeed) * pose.hoverAmp * u
  val jump = -abs(sin(t * 3.2f)) * pose.jump * 7f * u
  val tremble = (sin(t * 47f) * 0.7f + sin(t * 31f) * 0.4f) * pose.tremble * u
  val cx = cx0 + tremble
  val cy = cy0 + hover + jump
  val breath = sin(t * 2.1f)
  val sx = bounce * (1f - 0.018f * breath)
  val sy = bounce * (1f + 0.03f * breath)
  fun p(x: Float, y: Float) = Offset(cx + x * sx, cy + y * sy)

  // Expresión final (la risa del toque se impone a la emoción).
  val happyEye = maxOf(pose.happyEye, laugh)
  val mouthOpen = maxOf(pose.mouthOpen, laugh)
  val mouth = pose.mouth + (1f - pose.mouth) * laugh
  val mouthTremble = pose.mouthTremble * (1f - laugh)

  val tipHeight = r * 1.15f * shape.tip * (1f + 0.07f * sin(t * 7.3f) * pose.stoke)
  val wobble = r * (0.24f + 0.12f * sin(t * 3.1f) * pose.stoke + 0.05f * sin(t * 8.7f) * pose.stoke)

  // 1 · Brillo (luz propia en oscuro; halo tenue en claro).
  val glowOpacity = pose.glow * if (dark) 0.55f else 0.22f
  drawCircle(
    brush = Brush.radialGradient(
      0f to pose.base.copy(alpha = glowOpacity),
      0.5f to pose.base.copy(alpha = glowOpacity * 0.35f),
      1f to Color.Transparent,
      center = Offset(cx, cy - 0.3f * r),
      radius = r * 2.9f,
    ),
    radius = r * 2.9f,
    center = Offset(cx, cy - 0.3f * r),
  )

  // 2 · Sombra elíptica en el suelo (se encoge al subir).
  val ground = cy0 + r + legLength + 6f * u
  val lift = ((cy0 - cy) / (8f * u)).coerceIn(-0.4f, 1f)
  val shadowWidth = r * 1.35f * (1f - 0.35f * lift)
  val shadowColor = if (dark) Color.Black.copy(alpha = 0.5f) else pose.edge.copy(alpha = 0.28f)
  drawOval(
    brush = Brush.radialGradient(
      0f to shadowColor, 1f to Color.Transparent,
      center = Offset(cx0, ground), radius = shadowWidth,
    ),
    topLeft = Offset(cx0 - shadowWidth, ground - 0.14f * r),
    size = Size(shadowWidth * 2f, 0.28f * r),
  )

  // 3 · Llamas laterales (Fogata y Leyenda).
  if (shape.sideFlames > 0.01f) {
    for (side in intArrayOf(-1, 1)) {
      val angle = side * (16f + 5f * sin(t * 2.4f + side))
      val path = flamePath(
        origin = p(side * 1.02f * r, 0.98f * r), angleDeg = angle,
        radius = 0.34f * r * shape.sideFlames, height = 1.35f * r * shape.sideFlames * (1f + 0.08f * sin(t * 5.3f + side)),
        wobble = side * 0.12f * r * sin(t * 4.1f + side * 1.3f),
      )
      drawPath(
        path,
        brush = Brush.verticalGradient(
          0f to pose.core, 0.6f to pose.base, 1f to pose.edge,
          startY = cy - 1.4f * r, endY = cy + 0.5f * r,
        ),
      )
    }
  }

  // 4 · Piernas (detrás del cuerpo): cápsulas cortas con pie redondeado.
  val legColor = lerp(pose.base, pose.edge, 0.45f)
  val footColor = lerp(pose.base, pose.edge, 0.6f)
  for (side in intArrayOf(-1, 1)) {
    val kick = sin(t * pose.hoverSpeed * 1.4f + side) * 7f + pose.jump * 18f
    val a = Math.toRadians((side * (6f + kick * side)).toDouble()).toFloat()
    val hip = p(side * 0.34f * r, 0.8f * r)
    val length = legLength * (1f - 0.18f * pose.jump * abs(sin(t * 3.2f)))
    val foot = Offset(hip.x + sin(a) * length, hip.y + cos(a) * length)
    drawLine(legColor, hip, foot, strokeWidth = 0.24f * r * ext, cap = StrokeCap.Round)
    val footWidth = 0.36f * r * ext
    drawOval(
      footColor,
      topLeft = Offset(foot.x - footWidth / 2f + side * 0.07f * r, foot.y - 0.08f * r),
      size = Size(footWidth, 0.2f * r * ext),
    )
  }

  // 5 · Cuerpo: gota de fuego con volumen + dos lenguas secundarias unidas a la silueta
  // (tres puntas = se lee como fuego, no como gota de agua).
  val flameTongues = shape.tip.coerceAtMost(1f)
  val body = Path().apply {
    op(
      bodyPath(::p, r, tipHeight, wobble),
      Path().apply {
        op(
          flamePath(
            p(-0.5f * r, -0.42f * r), angleDeg = -24f + 5f * sin(t * 3.7f) * pose.stoke,
            radius = 0.26f * r, height = 1.05f * r * flameTongues * (1f + 0.08f * sin(t * 6.1f) * pose.stoke),
            wobble = -0.08f * r * sin(t * 4.3f),
          ),
          flamePath(
            p(0.56f * r, -0.4f * r), angleDeg = 26f + 5f * sin(t * 4.1f + 1f) * pose.stoke,
            radius = 0.2f * r, height = 0.78f * r * flameTongues * (1f + 0.1f * sin(t * 7.7f + 2f) * pose.stoke),
            wobble = 0.06f * r * sin(t * 5.1f),
          ),
          PathOperation.Union,
        )
      },
      PathOperation.Union,
    )
  }
  drawPath(
    body,
    brush = Brush.radialGradient(
      0f to pose.core, 0.42f to pose.base, 1f to pose.edge,
      center = p(-0.32f * r, -0.3f * r), radius = r * 1.8f * bounce,
    ),
  )
  // Punta encendida: llama interior más clara que se desvanece antes de la cara.
  val tipY = -(r + tipHeight)
  clipPath(body) {
    drawPath(
      bodyPath(::p, r * 0.62f, tipHeight * 1.05f, wobble * 1.1f, offsetY = -0.32f * r),
      brush = Brush.verticalGradient(
        0f to pose.core.copy(alpha = 0.7f), 0.5f to pose.core.copy(alpha = 0.16f), 1f to Color.Transparent,
        startY = p(0f, tipY).y, endY = p(0f, -0.25f * r).y,
      ),
    )
  }
  // Borde: más marcado en claro (sombra en vez de brillo).
  drawPath(body, color = pose.edge.copy(alpha = if (dark) 0.55f else 0.9f), style = Stroke(width = 0.045f * r))
  // Brillo especular arriba a la izquierda.
  val specular = p(-0.5f * r, -0.48f * r)
  rotate(-32f, pivot = specular) {
    drawOval(
      brush = Brush.radialGradient(
        0f to Color.White.copy(alpha = 0.5f), 1f to Color.White.copy(alpha = 0f),
        center = specular, radius = 0.26f * r,
      ),
      topLeft = Offset(specular.x - 0.13f * r, specular.y - 0.25f * r),
      size = Size(0.26f * r, 0.5f * r),
    )
  }
  drawCircle(Color.White.copy(alpha = 0.45f), radius = 0.04f * r, center = p(-0.3f * r, -0.72f * r))

  // 6 · Brazos (delante): "manguera" curva con mano redonda.
  val armColor = lerp(pose.base, pose.edge, 0.12f)
  val handColor = lerp(pose.base, pose.core, 0.22f)
  for (side in intArrayOf(-1, 1)) {
    var angle = if (side < 0) pose.leftArm else pose.rightArm
    angle += sin(t * pose.hoverSpeed * 1.2f + side * 0.8f) * pose.sway
    if (side > 0) angle += pose.wave * sin(t * 9f) * 22f
    angle += sin(t * 40f + side) * 4f * pose.tremble
    val a = Math.toRadians(angle.toDouble()).toFloat()
    val shoulder = p(side * 0.86f * r, 0.32f * r)
    val length = 0.64f * r * ext
    val dx = side * sin(a)
    val dy = cos(a)
    val hand = Offset(shoulder.x + dx * length, shoulder.y + dy * length)
    val control = Offset(
      (shoulder.x + hand.x) / 2f + (-dy) * side * 0.14f * r,
      (shoulder.y + hand.y) / 2f + dx * side * 0.14f * r,
    )
    val arm = Path().apply { moveTo(shoulder.x, shoulder.y); quadraticTo(control.x, control.y, hand.x, hand.y) }
    drawPath(arm, pose.edge.copy(alpha = 0.6f), style = Stroke(0.27f * r * ext, cap = StrokeCap.Round))
    drawPath(arm, armColor, style = Stroke(0.2f * r * ext, cap = StrokeCap.Round))
    drawCircle(pose.edge.copy(alpha = 0.6f), radius = 0.175f * r * ext, center = hand)
    drawCircle(handColor, radius = 0.145f * r * ext, center = hand)
  }

  // 7 · Cara.
  drawFace(::p, r, pose, t, blink, happyEye, mouthOpen, mouth, mouthTremble)

  // 8 · Lágrima (apagada) y gota de sudor (inquieta).
  if (pose.tear > 0.01f) {
    val f = (t * 0.45f) % 1f
    drawDrop(p(-0.4f * r, 0.16f * r + f * 0.5f * r), 0.07f * r, pose.tear * (1f - f))
  }
  if (pose.sweat > 0.01f) {
    val f = (t * 0.55f) % 1f
    drawDrop(p(0.82f * r, -0.72f * r + f * 0.35f * r), 0.09f * r, pose.sweat * (1f - f * 0.6f))
  }

  // 9 · Chispas (eufórica), corona y órbita (Leyenda).
  if (pose.sparks > 0.01f) {
    for (i in 0 until 6) {
      val angle = (i * 60f + t * 22f) * (PI.toFloat() / 180f)
      val rad = 1.9f * r + sin(t * 3f + i) * 0.12f * r
      val centerPoint = Offset(cx + cos(angle) * rad, cy - 0.2f * r + sin(angle) * rad * 0.62f)
      val starSize = 0.13f * r * (0.6f + 0.4f * sin(t * 5f + i * 1.7f))
      drawStar(centerPoint, starSize, if (i % 2 == 0) Tokens.Colors.Gold else Tokens.Colors.LightFire, pose.sparks)
    }
  }
  if (shape.crown > 0.01f) {
    drawCrown(p(wobble * 0.5f, tipY - 0.34f * r + sin(t * 2f) * 0.04f * r), r * 1.35f, shape.crown)
  }
  if (shape.orbit > 0.01f) {
    for (i in 0 until 3) {
      val angle = t * 1.4f + i * 2.094f
      val centerPoint = Offset(cx + cos(angle) * 2.05f * r, cy - 0.1f * r + sin(angle) * 0.55f * r)
      val starSize = 0.07f * r * (1.1f + 0.4f * sin(angle))
      drawCircle(
        brush = Brush.radialGradient(
          0f to Tokens.Colors.LightFire, 0.4f to Tokens.Colors.Gold.copy(alpha = 0.9f), 1f to Color.Transparent,
          center = centerPoint, radius = starSize * 2.6f,
        ),
        radius = starSize * 2.6f * shape.orbit, center = centerPoint,
      )
    }
  }
}

/** Gota de fuego del cuerpo. Origen en el centro del bulbo; punta hacia arriba con vaivén. */
private fun bodyPath(
  p: (Float, Float) -> Offset,
  r: Float,
  tipHeight: Float,
  wobble: Float,
  offsetY: Float = 0f,
): Path = Path().apply {
  val tipY = -r - tipHeight
  val start = p(wobble, tipY + offsetY)
  moveTo(start.x, start.y)
  val c1 = p(wobble * 0.6f + 0.2f * r, -r - tipHeight * 0.55f + offsetY)
  val c2 = p(1.04f * r, -0.8f * r + offsetY)
  val e1 = p(r, 0.05f * r + offsetY)
  cubicTo(c1.x, c1.y, c2.x, c2.y, e1.x, e1.y)
  val c3 = p(r, 0.6f * r + offsetY); val c4 = p(0.56f * r, r + offsetY); val e2 = p(0f, r + offsetY)
  cubicTo(c3.x, c3.y, c4.x, c4.y, e2.x, e2.y)
  val c5 = p(-0.56f * r, r + offsetY); val c6 = p(-r, 0.6f * r + offsetY); val e3 = p(-r, 0.05f * r + offsetY)
  cubicTo(c5.x, c5.y, c6.x, c6.y, e3.x, e3.y)
  val c7 = p(-1.02f * r, -0.85f * r + offsetY); val c8 = p(wobble * 0.4f - 0.5f * r, -r - tipHeight * 0.4f + offsetY)
  val e4 = p(wobble, tipY + offsetY)
  cubicTo(c7.x, c7.y, c8.x, c8.y, e4.x, e4.y)
  close()
}

/** Lengua de fuego genérica (llamas laterales, corona). Base en [origen], crece en dirección [angulo]. */
private fun flamePath(origin: Offset, angleDeg: Float, radius: Float, height: Float, wobble: Float): Path {
  val a = Math.toRadians(angleDeg.toDouble())
  val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
  // Local: base en (0,0), punta en (vaiven, -alto). Rota y traslada.
  fun m(x: Float, y: Float) = Offset(origin.x + x * ca - y * sa, origin.y + x * sa + y * ca)
  val r = radius
  return Path().apply {
    val tip = m(wobble, -height); moveTo(tip.x, tip.y)
    var c1 = m(wobble * 0.5f + r * 0.3f, -height * 0.6f); var c2 = m(r * 1.05f, -r * 1.7f); var e = m(r, -r)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    c1 = m(r, -r * 0.4f); c2 = m(r * 0.55f, 0f); e = m(0f, 0f)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    c1 = m(-r * 0.55f, 0f); c2 = m(-r, -r * 0.4f); e = m(-r, -r)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    c1 = m(-r * 1.05f, -r * 1.7f); c2 = m(wobble * 0.5f - r * 0.5f, -height * 0.55f); e = m(wobble, -height)
    cubicTo(c1.x, c1.y, c2.x, c2.y, e.x, e.y)
    close()
  }
}

private fun DrawScope.drawFace(
  p: (Float, Float) -> Offset,
  r: Float,
  pose: Pose,
  t: Float,
  blink: Float,
  happyEye: Float,
  mouthOpen: Float,
  mouth: Float,
  mouthTremble: Float,
) {
  val ink = Tokens.Colors.Pupil
  val rx = 0.15f * r
  val ry = (0.2f * r * pose.eyeOpen * (1f - blink)).coerceAtLeast(0.014f * r)

  for (side in intArrayOf(-1, 1)) {
    val c = p(side * 0.39f * r, -0.02f * r)

    // Ojo normal (con párpado caído si está apagada).
    val eyeOpacity = 1f - happyEye
    if (eyeOpacity > 0.01f) {
      var eye = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c.x - rx, c.y - ry, c.x + rx, c.y + ry)) }
      if (pose.droopyEye > 0.01f) {
        val outer = side * rx * 1.6f
        val inner = -side * rx * 1.6f
        val yExt = c.y - ry + pose.droopyEye * ry * 1.15f
        val yInt = c.y - ry + pose.droopyEye * ry * 0.5f
        val eyelid = Path().apply {
          moveTo(c.x + outer, yExt); lineTo(c.x + inner, yInt)
          lineTo(c.x + inner, c.y - ry * 3f); lineTo(c.x + outer, c.y - ry * 3f); close()
        }
        eye = Path().apply { op(eye, eyelid, PathOperation.Difference) }
        drawLine(
          ink.copy(alpha = eyeOpacity * pose.droopyEye),
          Offset(c.x + side * rx * 1.05f, yExt + side * 0f), Offset(c.x - side * rx * 1.05f, yInt),
          strokeWidth = 0.045f * r, cap = StrokeCap.Round,
        )
      }
      drawPath(eye, ink.copy(alpha = eyeOpacity))
      val reflectionOpacity = eyeOpacity * (1f - pose.droopyEye * 1.3f).coerceIn(0f, 1f) * (1f - blink)
      if (reflectionOpacity > 0.01f) {
        drawCircle(Color.White.copy(alpha = reflectionOpacity), radius = 0.048f * r * pose.eyeOpen.coerceAtMost(1.15f),
          center = Offset(c.x + 0.035f * r, c.y - 0.06f * r * pose.eyeOpen))
        drawCircle(Color.White.copy(alpha = reflectionOpacity * 0.7f), radius = 0.022f * r,
          center = Offset(c.x - 0.04f * r, c.y + 0.05f * r * pose.eyeOpen))
      }
    }
    // Ojos felices "^ ^".
    if (happyEye > 0.01f) {
      val arc = Path().apply {
        moveTo(c.x - rx * 1.1f, c.y + 0.04f * r)
        quadraticTo(c.x, c.y - 0.22f * r, c.x + rx * 1.1f, c.y + 0.04f * r)
      }
      drawPath(arc, ink.copy(alpha = happyEye), style = Stroke(0.065f * r, cap = StrokeCap.Round))
    }
    // Cejas de preocupación (extremo interior hacia arriba).
    if (pose.browVisible > 0.01f) {
      val cc = Offset(c.x, c.y - 0.29f * r - 0.05f * r * pose.eyeOpen)
      drawLine(
        ink.copy(alpha = pose.browVisible),
        Offset(cc.x - side * 0.12f * r, cc.y - pose.brow * 0.06f * r),
        Offset(cc.x + side * 0.12f * r, cc.y + pose.brow * 0.04f * r),
        strokeWidth = 0.05f * r, cap = StrokeCap.Round,
      )
    }
    // Mejillas.
    if (pose.cheeks > 0.01f) {
      val m = p(side * 0.64f * r, 0.24f * r)
      drawOval(
        brush = Brush.radialGradient(
          0f to Tokens.Colors.Cheek.copy(alpha = 0.65f * pose.cheeks), 1f to Color.Transparent,
          center = m, radius = 0.13f * r,
        ),
        topLeft = Offset(m.x - 0.13f * r, m.y - 0.075f * r), size = Size(0.26f * r, 0.15f * r),
      )
    }
  }

  // Boca.
  val mb = p(0f, 0.3f * r)
  val width = 0.21f * r
  val lineOpacity = (1f - mouthOpen) * (1f - mouthTremble)
  if (lineOpacity > 0.01f) {
    val smile = Path().apply {
      moveTo(mb.x - width, mb.y); quadraticTo(mb.x, mb.y + mouth * 0.2f * r, mb.x + width, mb.y)
    }
    drawPath(smile, ink.copy(alpha = lineOpacity), style = Stroke(0.06f * r, cap = StrokeCap.Round))
  }
  if (mouthOpen > 0.01f) {
    val background = mb.y + 0.05f * r + 0.36f * r * mouthOpen
    val openMouth = Path().apply {
      moveTo(mb.x - width * 1.1f, mb.y - 0.02f * r)
      quadraticTo(mb.x, mb.y + 0.03f * r, mb.x + width * 1.1f, mb.y - 0.02f * r)
      quadraticTo(mb.x, background, mb.x - width * 1.1f, mb.y - 0.02f * r)
      close()
    }
    drawPath(openMouth, Tokens.Colors.Mouth.copy(alpha = mouthOpen))
    clipPath(openMouth) {
      drawCircle(Tokens.Colors.Tongue.copy(alpha = mouthOpen), radius = 0.12f * r,
        center = Offset(mb.x + 0.03f * r, background - 0.02f * r))
    }
  }
  if (mouthTremble > 0.01f) {
    val ripple = Path()
    val steps = 16
    for (i in 0..steps) {
      val f = i / steps.toFloat()
      val x = mb.x - width + f * width * 2f
      val y = mb.y + 0.02f * r + sin(f * PI.toFloat() * 5f + t * 12f) * 0.035f * r
      if (i == 0) ripple.moveTo(x, y) else ripple.lineTo(x, y)
    }
    drawPath(ripple, ink.copy(alpha = mouthTremble), style = Stroke(0.05f * r, cap = StrokeCap.Round, join = StrokeJoin.Round))
  }
}

/** Gota de agua (lágrima o sudor) con reflejo. */
private fun DrawScope.drawDrop(c: Offset, radius: Float, opacity: Float) {
  if (opacity <= 0.01f) return
  val dropPath = Path().apply {
    moveTo(c.x, c.y - radius * 2.3f)
    cubicTo(c.x + radius * 0.4f, c.y - radius * 1.4f, c.x + radius, c.y - radius * 0.6f, c.x + radius, c.y)
    cubicTo(c.x + radius, c.y + radius * 0.6f, c.x + radius * 0.55f, c.y + radius, c.x, c.y + radius)
    cubicTo(c.x - radius * 0.55f, c.y + radius, c.x - radius, c.y + radius * 0.6f, c.x - radius, c.y)
    cubicTo(c.x - radius, c.y - radius * 0.6f, c.x - radius * 0.4f, c.y - radius * 1.4f, c.x, c.y - radius * 2.3f)
    close()
  }
  drawPath(dropPath, Tokens.Colors.Water.copy(alpha = opacity))
  drawCircle(Color.White.copy(alpha = opacity * 0.8f), radius = radius * 0.3f, center = Offset(c.x - radius * 0.35f, c.y - radius * 0.1f))
}

/** Estrella de cuatro puntas (chispa de celebración). */
internal fun DrawScope.drawStar(c: Offset, starSize: Float, color: Color, opacity: Float) {
  val e = Path().apply {
    moveTo(c.x, c.y - starSize)
    quadraticTo(c.x, c.y, c.x + starSize, c.y)
    quadraticTo(c.x, c.y, c.x, c.y + starSize)
    quadraticTo(c.x, c.y, c.x - starSize, c.y)
    quadraticTo(c.x, c.y, c.x, c.y - starSize)
    close()
  }
  drawCircle(
    brush = Brush.radialGradient(0f to color.copy(alpha = 0.45f * opacity), 1f to Color.Transparent, center = c, radius = starSize * 1.6f),
    radius = starSize * 1.6f, center = c,
  )
  drawPath(e, color.copy(alpha = opacity))
}

/** Corona de fuego dorada (Leyenda): tres llamas y una banda. */
private fun DrawScope.drawCrown(c: Offset, r: Float, opacity: Float) {
  val gold = Brush.verticalGradient(
    0f to Tokens.Colors.LightFire, 1f to Tokens.Colors.Gold,
    startY = c.y - 0.5f * r, endY = c.y + 0.05f * r,
  )
  drawCircle(
    brush = Brush.radialGradient(0f to Tokens.Colors.Gold.copy(alpha = 0.5f * opacity), 1f to Color.Transparent, center = c, radius = 0.7f * r),
    radius = 0.7f * r, center = c,
  )
  val tips = listOf(Triple(-0.2f, -18f, 0.28f), Triple(0.2f, 18f, 0.28f), Triple(0f, 0f, 0.4f))
  for ((dx, angle, height) in tips) {
    drawPath(
      flamePath(Offset(c.x + dx * r, c.y), angle, radius = 0.085f * r, height = height * r, wobble = 0f),
      brush = gold, alpha = opacity,
    )
  }
  drawRoundRect(
    brush = gold, alpha = opacity,
    topLeft = Offset(c.x - 0.3f * r, c.y - 0.06f * r), size = Size(0.6f * r, 0.11f * r),
    cornerRadius = androidx.compose.ui.geometry.CornerRadius(0.055f * r),
  )
}
