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
data class Capabilities(val steps: Boolean, val heartRate: Boolean)

/** Eventos de salud que llegan al ViewModel (de Health Services o del simulador de depuración). */
sealed interface HealthEvent {
  /** Pasos totales de hoy. */
  data class Steps(val today: Int) : HealthEvent
  data class HeartRate(val bpm: Int, val ms: Long) : HealthEvent
}

/** Canal común: Health Services y el simulador (adb) publican aquí. */
object HealthChannel {
  val events = MutableSharedFlow<HealthEvent>(extraBufferCapacity = 64)
}

/** Permiso de pulso según versión (docs: BODY_SENSORS ≤ API 35, health.READ_HEART_RATE ≥ API 36). */
val heartRatePermission: String =
  if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

val healthPermissions = arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, heartRatePermission)

fun hasPermission(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

/**
 * Health Services en modo pasivo: pasos diarios y pulso mientras la app está abierta (callback).
 * Limitación documentada: sin PassiveListenerService no se reciben datos con la app cerrada.
 */
class HealthRepository(private val ctx: Context) {
  private val client = HealthServices.getClient(ctx).passiveMonitoringClient
  val events: SharedFlow<HealthEvent> = HealthChannel.events

  suspend fun capabilities(): Capabilities = try {
    val c = client.getCapabilitiesAsync().await()
    Log.i(TAG, "pasivo soporta: ${c.supportedDataTypesPassiveMonitoring.map { it.name }}")
    Capabilities(
      steps = DataType.STEPS_DAILY in c.supportedDataTypesPassiveMonitoring,
      heartRate = DataType.HEART_RATE_BPM in c.supportedDataTypesPassiveMonitoring,
    )
  } catch (e: Exception) {
    // Reloj sin Health Services: la app sigue en modo manual.
    Log.w(TAG, "Health Services no disponible")
    Capabilities(steps = false, heartRate = false)
  }

  /** Registra solo los tipos disponibles y con permiso. Devuelve qué quedó activo. */
  suspend fun start(): Capabilities {
    val cap = capabilities()
    val steps = cap.steps && hasPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION)
    val heartRate = cap.heartRate && hasPermission(ctx, heartRatePermission)
    val types = buildSet {
      if (steps) add(DataType.STEPS_DAILY)
      if (heartRate) add(DataType.HEART_RATE_BPM)
    }
    if (types.isEmpty()) return Capabilities(false, false)
    val config = PassiveListenerConfig.builder().setDataTypes(types).build()
    client.setPassiveListenerCallback(config, callback)
    return Capabilities(steps, heartRate)
  }

  suspend fun stop() {
    try { client.clearPassiveListenerCallbackAsync().await() } catch (_: Exception) {}
  }

  private val callback = object : PassiveListenerCallback {
    override fun onNewDataPointsReceived(dataPoints: DataPointContainer) {
      // Los datos llegan en lotes: se toma el último valor de pasos y cada lectura de pulso.
      dataPoints.getData(DataType.STEPS_DAILY).lastOrNull()?.let {
        HealthChannel.events.tryEmit(HealthEvent.Steps(it.value.toInt()))
      }
      val currentTime = System.currentTimeMillis()
      dataPoints.getData(DataType.HEART_RATE_BPM).forEach {
        if (it.value > 0) HealthChannel.events.tryEmit(HealthEvent.HeartRate(it.value.toInt(), currentTime))
      }
    }
  }

  companion object { private const val TAG = "BrasaSalud" }
}
