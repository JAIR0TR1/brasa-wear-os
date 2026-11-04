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
class SimuladorReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.hasExtra("pasos")) CanalSalud.eventos.tryEmit(EventoSalud.Pasos(intent.getIntExtra("pasos", 0)))
    if (intent.hasExtra("pulso")) {
      val bpm = intent.getIntExtra("pulso", 0)
      // Tres lecturas seguidas: la regla de tensión exige al menos 2 en la ventana.
      val ahora = System.currentTimeMillis()
      for (i in 2 downTo 0) CanalSalud.eventos.tryEmit(EventoSalud.Pulso(bpm, ahora - i * 30_000L))
    }
  }
}
