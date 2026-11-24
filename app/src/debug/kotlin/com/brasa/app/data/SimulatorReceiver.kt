package com.brasa.app.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * SOLO en depuración. Simula sensores desde adb, porque en Wear OS 4+ los datos sintéticos
 * del emulador están ligados a sesiones de ejercicio y no alimentan el modo pasivo.
 *
 *   adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pasos 6500
 *   adb shell am broadcast -a com.brasa.app.SIMULAR -p com.brasa.app --ei pulso 112
 */
class SimulatorReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.hasExtra("pasos")) HealthChannel.events.tryEmit(HealthEvent.Steps(intent.getIntExtra("pasos", 0)))
    if (intent.hasExtra("pulso")) {
      val bpm = intent.getIntExtra("pulso", 0)
      // Tres lecturas a 70 s (se guarda 1 por minuto; la regla exige ≥ 2 en 3 min).
      val currentTime = System.currentTimeMillis()
      for (i in 2 downTo 0) HealthChannel.events.tryEmit(HealthEvent.HeartRate(bpm, currentTime - i * 70_000L))
    }
  }
}
