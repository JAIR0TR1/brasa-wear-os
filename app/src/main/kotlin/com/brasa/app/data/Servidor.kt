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
enum class Situacion(val api: String) {
  SALUDO("saludo"), INACTIVIDAD("inactividad"), TENSION("tension"),
  POST_RESPIRACION("post_respiracion"), META_CUMPLIDA("meta_cumplida"), SUBIDA_NIVEL("subida_nivel"),
}

/** Nivel agregado: lo único que sale del reloj sobre la actividad (nunca pasos exactos). */
enum class NivelActividad(val api: String) { BAJO("bajo"), MEDIO("medio"), ALTO("alto") }

fun nivelDe(progreso: Float) = when {
  progreso < 0.3f -> NivelActividad.BAJO
  progreso < 0.8f -> NivelActividad.MEDIO
  else -> NivelActividad.ALTO
}

/**
 * Cliente HTTP ligero (HttpURLConnection, sin librerías). Timeouts cortos y nunca lanza:
 * cualquier fallo devuelve null y la app usa sus frases locales.
 */
class ClienteServidor(private val base: String = BuildConfig.SERVER_URL.trimEnd('/')) {
  val configurado get() = base.startsWith("http")

  suspend fun mensaje(
    dispositivo: String, idioma: String, situacion: Situacion, actividad: NivelActividad, emocion: String, etapa: String,
  ): String? = post(
    "/api/mensaje",
    buildJsonObject {
      put("dispositivo", dispositivo); put("idioma", idioma); put("situacion", situacion.api)
      put("actividad", actividad.api); put("emocion", emocion); put("etapa", etapa)
    },
  )?.get("frase")?.jsonPrimitive?.content

  suspend fun meta(dispositivo: String, idioma: String, tendencia: String, constancia: NivelActividad, media: NivelActividad): String? = post(
    "/api/meta",
    buildJsonObject {
      put("dispositivo", dispositivo); put("idioma", idioma); put("tendencia", tendencia)
      put("constancia", constancia.api); put("actividadMedia", media.api)
    },
  )?.get("frase")?.jsonPrimitive?.content

  suspend fun resumen(
    dispositivo: String, idioma: String, actividad: NivelActividad, metaCumplida: Boolean,
    respiraciones: Int, tension: String, emocionFinal: String,
  ): List<String>? = post(
    "/api/resumen",
    buildJsonObject {
      put("dispositivo", dispositivo); put("idioma", idioma); put("actividad", actividad.api)
      put("metaCumplida", metaCumplida); put("respiraciones", respiraciones.coerceIn(0, 20))
      put("tension", tension); put("emocionFinal", emocionFinal)
    },
  )?.get("frases")?.jsonArray?.map { it.jsonPrimitive.content }

  private suspend fun post(ruta: String, cuerpo: JsonObject): JsonObject? = withContext(Dispatchers.IO) {
    if (!configurado) return@withContext null
    var conexion: HttpURLConnection? = null
    try {
      conexion = (URL(base + ruta).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 3_000
        readTimeout = 9_000
        doOutput = true
        setRequestProperty("content-type", "application/json")
      }
      conexion.outputStream.use { it.write(cuerpo.toString().toByteArray()) }
      if (conexion.responseCode !in 200..299) return@withContext null
      val texto = conexion.inputStream.bufferedReader().use { it.readText() }
      Json.parseToJsonElement(texto).jsonObject
    } catch (e: Exception) {
      Log.i("BrasaServidor", "sin servidor: ${e.javaClass.simpleName}") // sin datos
      null
    } finally {
      conexion?.disconnect()
    }
  }
}
