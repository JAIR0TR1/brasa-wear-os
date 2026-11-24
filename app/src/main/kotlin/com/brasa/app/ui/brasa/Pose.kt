package com.brasa.app.ui.brasa

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.brasa.app.ui.theme.Tokens

/** [key] es el valor histórico que se guarda en preferencias y que (en minúsculas) entiende el servidor: no cambiar. */
enum class Emotion(val key: String) {
  DIM("APAGADA"), RESTLESS("INQUIETA"), CALM("TRANQUILA"), HAPPY("FELIZ"), ECSTATIC("EUFORICA"),
}

/** Etapas de evolución por nivel. */
/** [key]: igual que en Emocion, valor histórico para preferencias y servidor. */
enum class Stage(val minLevel: Int, val key: String) {
  SPARK(1, "CHISPA"), FLAME(5, "LLAMA"), BONFIRE(10, "FOGATA"), LEGEND(20, "LEYENDA");

  companion object {
    fun fromLevel(level: Int): Stage = entries.last { level >= it.minLevel }
  }
}

/**
 * Parámetros continuos del personaje. Cambiar de emoción = interpolar de una pose a otra,
 * así toda transición queda animada sin dibujos separados.
 * Ángulos de brazos en grados desde "colgando": positivo = hacia fuera/arriba, negativo = hacia dentro.
 */
@Immutable
data class Pose(
  val base: Color,
  val core: Color,
  val edge: Color,
  val scale: Float,
  val eyeOpen: Float,
  val happyEye: Float,
  val droopyEye: Float,
  val brow: Float,
  val browVisible: Float,
  val mouth: Float,
  val mouthOpen: Float,
  val mouthTremble: Float,
  val cheeks: Float,
  val leftArm: Float,
  val rightArm: Float,
  val wave: Float,
  val sway: Float,
  val hoverAmp: Float,
  val hoverSpeed: Float,
  val jump: Float,
  val tremble: Float,
  val stoke: Float,
  val tear: Float,
  val sweat: Float,
  val sparks: Float,
  val glow: Float,
)

/** Forma por etapa de evolución (se interpola igual que la pose). */
@Immutable
data class BrasaShape(
  val scale: Float,
  val tip: Float,
  val limbs: Float,
  val sideFlames: Float,
  val crown: Float,
  val orbit: Float,
)

fun shapeFor(stage: Stage): BrasaShape = when (stage) {
  Stage.SPARK -> BrasaShape(scale = 0.74f, tip = 0.22f, limbs = 0.82f, sideFlames = 0f, crown = 0f, orbit = 0f)
  Stage.FLAME -> BrasaShape(scale = 0.9f, tip = 1f, limbs = 1f, sideFlames = 0f, crown = 0f, orbit = 0f)
  Stage.BONFIRE -> BrasaShape(scale = 0.98f, tip = 1.08f, limbs = 1f, sideFlames = 1f, crown = 0f, orbit = 0f)
  Stage.LEGEND -> BrasaShape(scale = 0.96f, tip = 1.12f, limbs = 1f, sideFlames = 1f, crown = 1f, orbit = 1f)
}

fun poseFor(emotion: Emotion): Pose {
  val p = Tokens.palette(emotion, dark = true)
  return when (emotion) {
    Emotion.DIM -> Pose(
      p.base, p.core, p.edge, scale = 0.84f,
      eyeOpen = 0.85f, happyEye = 0f, droopyEye = 0.62f, brow = 0.7f, browVisible = 0.9f,
      mouth = -0.75f, mouthOpen = 0f, mouthTremble = 0f, cheeks = 0f,
      leftArm = 6f, rightArm = 6f, wave = 0f, sway = 3f,
      hoverAmp = 1.2f, hoverSpeed = 0.9f, jump = 0f, tremble = 0f, stoke = 0.25f,
      tear = 1f, sweat = 0f, sparks = 0f, glow = 0.35f,
    )
    Emotion.RESTLESS -> Pose(
      p.base, p.core, p.edge, scale = 0.96f,
      eyeOpen = 1.32f, happyEye = 0f, droopyEye = 0f, brow = 1f, browVisible = 1f,
      mouth = 0f, mouthOpen = 0f, mouthTremble = 1f, cheeks = 0f,
      leftArm = -78f, rightArm = -78f, wave = 0f, sway = 4f,
      hoverAmp = 1.6f, hoverSpeed = 2.6f, jump = 0f, tremble = 1f, stoke = 1.4f,
      tear = 0f, sweat = 1f, sparks = 0f, glow = 0.75f,
    )
    Emotion.CALM -> Pose(
      p.base, p.core, p.edge, scale = 0.95f,
      eyeOpen = 1f, happyEye = 0f, droopyEye = 0f, brow = 0f, browVisible = 0f,
      mouth = 0.55f, mouthOpen = 0f, mouthTremble = 0f, cheeks = 0.35f,
      leftArm = 26f, rightArm = 26f, wave = 0f, sway = 7f,
      hoverAmp = 2.2f, hoverSpeed = 1.3f, jump = 0f, tremble = 0f, stoke = 0.7f,
      tear = 0f, sweat = 0f, sparks = 0f, glow = 0.7f,
    )
    Emotion.HAPPY -> Pose(
      p.base, p.core, p.edge, scale = 1f,
      eyeOpen = 1.22f, happyEye = 0f, droopyEye = 0f, brow = 0f, browVisible = 0f,
      mouth = 1f, mouthOpen = 0.75f, mouthTremble = 0f, cheeks = 1f,
      leftArm = 34f, rightArm = 148f, wave = 1f, sway = 5f,
      hoverAmp = 2.6f, hoverSpeed = 1.9f, jump = 0f, tremble = 0f, stoke = 1f,
      tear = 0f, sweat = 0f, sparks = 0f, glow = 0.9f,
    )
    Emotion.ECSTATIC -> Pose(
      p.base, p.core, p.edge, scale = 1.04f,
      eyeOpen = 1f, happyEye = 1f, droopyEye = 0f, brow = 0f, browVisible = 0f,
      mouth = 1f, mouthOpen = 1f, mouthTremble = 0f, cheeks = 1f,
      leftArm = 158f, rightArm = 158f, wave = 0f, sway = 12f,
      hoverAmp = 1f, hoverSpeed = 3.2f, jump = 1f, tremble = 0f, stoke = 1.5f,
      tear = 0f, sweat = 0f, sparks = 1f, glow = 1f,
    )
  }
}

private fun l(a: Float, b: Float, t: Float) = a + (b - a) * t

fun lerpPose(a: Pose, b: Pose, t: Float) = Pose(
  base = lerp(a.base, b.base, t), core = lerp(a.core, b.core, t), edge = lerp(a.edge, b.edge, t),
  scale = l(a.scale, b.scale, t), eyeOpen = l(a.eyeOpen, b.eyeOpen, t), happyEye = l(a.happyEye, b.happyEye, t),
  droopyEye = l(a.droopyEye, b.droopyEye, t), brow = l(a.brow, b.brow, t), browVisible = l(a.browVisible, b.browVisible, t),
  mouth = l(a.mouth, b.mouth, t), mouthOpen = l(a.mouthOpen, b.mouthOpen, t), mouthTremble = l(a.mouthTremble, b.mouthTremble, t),
  cheeks = l(a.cheeks, b.cheeks, t), leftArm = l(a.leftArm, b.leftArm, t), rightArm = l(a.rightArm, b.rightArm, t),
  wave = l(a.wave, b.wave, t), sway = l(a.sway, b.sway, t), hoverAmp = l(a.hoverAmp, b.hoverAmp, t),
  hoverSpeed = l(a.hoverSpeed, b.hoverSpeed, t), jump = l(a.jump, b.jump, t), tremble = l(a.tremble, b.tremble, t),
  stoke = l(a.stoke, b.stoke, t), tear = l(a.tear, b.tear, t), sweat = l(a.sweat, b.sweat, t),
  sparks = l(a.sparks, b.sparks, t), glow = l(a.glow, b.glow, t),
)

fun lerpShape(a: BrasaShape, b: BrasaShape, t: Float) = BrasaShape(
  scale = l(a.scale, b.scale, t), tip = l(a.tip, b.tip, t), limbs = l(a.limbs, b.limbs, t),
  sideFlames = l(a.sideFlames, b.sideFlames, t), crown = l(a.crown, b.crown, t), orbit = l(a.orbit, b.orbit, t),
)
