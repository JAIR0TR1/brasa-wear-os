package com.brasa.app.data

import android.util.Log
import com.brasa.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

/** Situaciones que el servidor sabe redactar (mismos valores que el esquema Zod). */
enum class Situation(val api: String) {
  GREETING("saludo"), INACTIVITY("inactividad"), TENSION("tension"),
  POST_BREATHING("post_respiracion"), GOAL_REACHED("meta_cumplida"), LEVEL_UP("subida_nivel"),
}

/** Nivel agregado: lo único que sale del reloj sobre la actividad (nunca pasos exactos). */
enum class ActivityLevel(val api: String) { LOW("bajo"), MEDIUM("medio"), HIGH("alto") }

fun levelOf(progress: Float) = when {
  progress < 0.3f -> ActivityLevel.LOW
  progress < 0.8f -> ActivityLevel.MEDIUM
  else -> ActivityLevel.HIGH
}

/**
 * Cliente HTTP ligero (HttpURLConnection, sin librerías). Timeouts cortos y nunca lanza:
 * cualquier fallo devuelve null y la app usa sus frases locales.
 */
class ServerClient(private val base: String = BuildConfig.SERVER_URL.trimEnd('/')) {
  private companion object { const val TAG = "BrasaServidor" }
  val configured get() = base.startsWith("http")

  suspend fun message(
    deviceId: String, language: String, situation: Situation, activity: ActivityLevel, emotion: String, stage: String,
  ): String? = post(
    "/api/mensaje",
    buildJsonObject {
      put("dispositivo", deviceId); put("idioma", language); put("situacion", situation.api)
      put("actividad", activity.api); put("emocion", emotion); put("etapa", stage)
    },
  )?.get("frase")?.jsonPrimitive?.content

  suspend fun goal(deviceId: String, language: String, trend: String, consistency: ActivityLevel, medium: ActivityLevel): String? = post(
    "/api/meta",
    buildJsonObject {
      put("dispositivo", deviceId); put("idioma", language); put("tendencia", trend)
      put("constancia", consistency.api); put("actividadMedia", medium.api)
    },
  )?.get("frase")?.jsonPrimitive?.content

  suspend fun summary(
    deviceId: String, language: String, activity: ActivityLevel, goalReached: Boolean,
    breaths: Int, tension: String, finalEmotion: String,
  ): List<String>? = post(
    "/api/resumen",
    buildJsonObject {
      put("dispositivo", deviceId); put("idioma", language); put("actividad", activity.api)
      put("metaCumplida", goalReached); put("respiraciones", breaths.coerceIn(0, 20))
      put("tension", tension); put("emocionFinal", finalEmotion)
    },
  )?.get("frases")?.jsonArray?.map { it.jsonPrimitive.content }

  private suspend fun post(route: String, body: JsonObject): JsonObject? = withContext(Dispatchers.IO) {
    if (!configured) return@withContext null
    var connection: HttpURLConnection? = null
    try {
      connection = (URL(base + route).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 3_000
        readTimeout = 9_000
        doOutput = true
        setRequestProperty("content-type", "application/json")
      }
      connection.outputStream.use { it.write(body.toString().toByteArray()) }
      if (connection.responseCode !in 200..299) { Log.i(TAG, "$route http ${connection.responseCode}"); return@withContext null }
      val text = connection.inputStream.bufferedReader().use { it.readText() }
      Json.parseToJsonElement(text).jsonObject.also {
        // Solo la ruta y el origen (ia/local): ni la frase ni datos del usuario.
        Log.i(TAG, "$route ok origen=${it["origen"]?.jsonPrimitive?.content}")
      }
    } catch (e: Exception) {
      Log.i(TAG, "sin servidor: ${e.javaClass.simpleName}") // sin datos
      null
    } finally {
      connection?.disconnect()
    }
  }
}
