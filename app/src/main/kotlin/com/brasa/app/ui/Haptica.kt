package com.brasa.app.ui

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Vibraciones con significado: cada patrón comunica algo (no hay vibración decorativa). */
class Haptica(ctx: Context) {
  private val v: Vibrator? = ctx.getSystemService(Vibrator::class.java)
  private val amplitud get() = v?.hasAmplitudeControl() == true

  private fun efecto(e: VibrationEffect) {
    if (v?.hasVibrator() == true) v.vibrate(e)
  }

  /** Toque a Brasa / selección. */
  fun toque() = efecto(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

  /** Nacimiento de Brasa en el arranque. */
  fun nacer() = efecto(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

  /** Inicio de la inhalación: pulso que crece. */
  fun inhala() = efecto(
    if (amplitud) VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 90), intArrayOf(0, 50, 0, 110), -1)
    else VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE),
  )

  /** Inicio de la exhalación: un pulso largo y suave. */
  fun exhala() = efecto(
    if (amplitud) VibrationEffect.createOneShot(140, 45)
    else VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
  )

  /** Meta cumplida, subida de nivel o respiración completada. */
  fun celebrar() = efecto(
    if (amplitud) VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40, 60, 120), intArrayOf(0, 120, 0, 160, 0, 255), -1)
    else VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK),
  )
}

@Composable
fun rememberHaptica(): Haptica {
  val ctx = LocalContext.current
  return remember { Haptica(ctx) }
}
