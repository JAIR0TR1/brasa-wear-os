package com.brasa.app.ui

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Vibraciones con significado: cada patrón comunica algo (no hay vibración decorativa).
 * Respeta los ajustes del sistema: si la respuesta táctil está desactivada no vibra, y desde
 * API 33 se declara como uso táctil (el sistema aplica la intensidad elegida y el modo silencio).
 */
class Haptics(private val ctx: Context) {
  private val v: Vibrator? = ctx.getSystemService(Vibrator::class.java)
  private val amplitude get() = v?.hasAmplitudeControl() == true

  private val allowed: Boolean
    get() = Settings.System.getInt(ctx.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

  private fun effect(e: VibrationEffect) {
    if (v?.hasVibrator() != true || !allowed) return
    if (Build.VERSION.SDK_INT >= 33) v.vibrate(e, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
    else v.vibrate(e)
  }

  /** Toque a Brasa / selección. */
  fun tap() = effect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))

  /** Nacimiento de Brasa en el arranque. */
  fun birth() = effect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

  /** Inicio de la inhalación: pulso que crece. */
  fun inhale() = effect(
    if (amplitude) VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 90), intArrayOf(0, 50, 0, 110), -1)
    else VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE),
  )

  /** Inicio de la exhalación: un pulso largo y suave. */
  fun exhale() = effect(
    if (amplitude) VibrationEffect.createOneShot(140, 45)
    else VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK),
  )

  /** Meta cumplida, subida de nivel o respiración completada. */
  fun celebrate() = effect(
    if (amplitude) VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40, 60, 120), intArrayOf(0, 120, 0, 160, 0, 255), -1)
    else VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK),
  )
}

@Composable
fun rememberHaptics(): Haptics {
  val ctx = LocalContext.current
  return remember { Haptics(ctx) }
}
