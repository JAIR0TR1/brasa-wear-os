package com.brasa.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.PassiveListenerCallback
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.PassiveListenerConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.guava.await

/** Lo que el reloj puede medir (consultado en tiempo de ejecución). */
data class Capacidades(val pasos: Boolean, val pulso: Boolean)

/** Eventos de salud que llegan al ViewModel (de Health Services o del simulador de depuración). */
sealed interface EventoSalud {
  /** Pasos totales de hoy. */
  data class Pasos(val hoy: Int) : EventoSalud
  data class Pulso(val bpm: Int, val ms: Long) : EventoSalud
}

/** Canal común: Health Services y el simulador (adb) publican aquí. */
object CanalSalud {
  val eventos = MutableSharedFlow<EventoSalud>(extraBufferCapacity = 64)
}

/** Permiso de pulso según versión (docs: BODY_SENSORS ≤ API 35, health.READ_HEART_RATE ≥ API 36). */
val permisoPulso: String =
  if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

val permisosSalud = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, permisoPulso)

fun tienePermiso(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

/**
 * Health Services en modo pasivo: pasos diarios y pulso mientras la app está abierta (callback).
 * Limitación documentada: sin PassiveListenerService no se reciben datos con la app cerrada.
 */
class RepositorioSalud(private val ctx: Context) {
  private val cliente = HealthServices.getClient(ctx).passiveMonitoringClient
  val eventos: SharedFlow<EventoSalud> = CanalSalud.eventos

  suspend fun capacidades(): Capacidades = try {
    val c = cliente.getCapabilitiesAsync().await()
    Log.i(TAG, "pasivo soporta: ${c.supportedDataTypesPassiveMonitoring.map { it.name }}")
    Capacidades(
      pasos = DataType.STEPS_DAILY in c.supportedDataTypesPassiveMonitoring,
      pulso = DataType.HEART_RATE_BPM in c.supportedDataTypesPassiveMonitoring,
    )
  } catch (e: Exception) {
    // Reloj sin Health Services: la app sigue en modo manual.
    Log.w(TAG, "Health Services no disponible")
    Capacidades(pasos = false, pulso = false)
  }

  /** Registra solo los tipos disponibles y con permiso. Devuelve qué quedó activo. */
  suspend fun iniciar(): Capacidades {
    val cap = capacidades()
    val pasos = cap.pasos && tienePermiso(ctx, Manifest.permission.ACTIVITY_RECOGNITION)
    val pulso = cap.pulso && tienePermiso(ctx, permisoPulso)
    val tipos = buildSet {
      if (pasos) add(DataType.STEPS_DAILY)
      if (pulso) add(DataType.HEART_RATE_BPM)
    }
    if (tipos.isEmpty()) return Capacidades(false, false)
    val config = PassiveListenerConfig.builder().setDataTypes(tipos).build()
    cliente.setPassiveListenerCallback(config, callback)
    return Capacidades(pasos, pulso)
  }

  suspend fun detener() {
    try { cliente.clearPassiveListenerCallbackAsync().await() } catch (_: Exception) {}
  }

  private val callback = object : PassiveListenerCallback {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
      // Los datos llegan en lotes: se toma el último valor de pasos y cada lectura de pulso.
      dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.let {
        CanalSalud.eventos.tryEmit(EventoSalud.Pasos(it.value.toInt()))
      }
      val ahora = System.currentTimeMillis()
      dataPoints.getData(DataType.HEART_RATE_BPM).forEach {
        if (it.value > 0) CanalSalud.eventos.tryEmit(EventoSalud.Pulso(it.value.toInt(), ahora))
      }
    }
  }

  companion object { private const val TAG = "BrasaSalud" }
}
