package com.brasa.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.almacen by preferencesDataStore(name = "brasa")

/** Ajustes del usuario. El idioma lo guarda AppCompat (autoStoreLocales), no aquí. */
data class Ajustes(
  val oscuro: Boolean = true,
  val demo: Boolean = false,
  /** null = automática (según los datos). */
  val emocionDemo: Emocion? = null,
  val etapaDemo: Etapa? = null,
)

class Preferencias(private val ctx: Context) {
  private object K {
    val oscuro = booleanPreferencesKey("oscuro")
    val demo = booleanPreferencesKey("demo")
    val emocionDemo = stringPreferencesKey("emocion_demo")
    val etapaDemo = stringPreferencesKey("etapa_demo")
  }

  val ajustes: Flow<Ajustes> = ctx.almacen.data.map { p ->
    Ajustes(
      oscuro = p[K.oscuro] ?: true,
      demo = p[K.demo] ?: false,
      emocionDemo = p[K.emocionDemo]?.let { n -> Emocion.entries.firstOrNull { it.name == n } },
      etapaDemo = p[K.etapaDemo]?.let { n -> Etapa.entries.firstOrNull { it.name == n } },
    )
  }

  /** Id aleatorio por reloj (UUID). No identifica a la persona: no hay registro ni cuenta. */
  suspend fun dispositivo(): String {
    val clave = stringPreferencesKey("dispositivo")
    ctx.almacen.data.map { it[clave] }.firstOrNull()?.let { return it }
    val nuevo = java.util.UUID.randomUUID().toString()
    ctx.almacen.edit { it[clave] = nuevo }
    return nuevo
  }

  suspend fun oscuro(v: Boolean) = ctx.almacen.edit { it[K.oscuro] = v }
  suspend fun demo(v: Boolean) = ctx.almacen.edit { it[K.demo] = v }
  suspend fun emocionDemo(e: Emocion?) = ctx.almacen.edit { if (e == null) it.remove(K.emocionDemo) else it[K.emocionDemo] = e.name }
  suspend fun etapaDemo(e: Etapa?) = ctx.almacen.edit { if (e == null) it.remove(K.etapaDemo) else it[K.etapaDemo] = e.name }
}


/** Una respiración guiada completada (para la gráfica de pulso). */
@kotlinx.serialization.Serializable
data class Respiro(val ms: Long, val antes: Int?, val despues: Int?)

/** Datos de un día. Todo se queda en el reloj (privacidad: nunca se envía en bruto). */
@kotlinx.serialization.Serializable
data class Dia(
  val epochDay: Long,
  val pasos: Int = 0,
  val pasosPorHora: List<Int> = List(24) { 0 },
  /** Pulso del día, como mucho una muestra por minuto. */
  val pulsos: List<com.brasa.app.domain.Muestra> = emptyList(),
  val respiros: List<Respiro> = emptyList(),
  val ultimoMovimientoMs: Long = 0,
)

@kotlinx.serialization.Serializable
data class Registro(
  val hoy: Dia,
  /** Pasos de los últimos 7 días completos (el último = ayer). */
  val historial: List<Int> = emptyList(),
  val meta: Int = com.brasa.app.domain.Reglas.META_INICIAL,
  /** Meta del día anterior, para que la IA explique el cambio. */
  val metaAnterior: Int = com.brasa.app.domain.Reglas.META_INICIAL,
  /** XP acumulada de días anteriores. */
  val xpPrevio: Int = 0,
  val respirosTotales: Int = 0,
  val permisosPreguntados: Boolean = false,
)

/** Persistencia del registro y cambio de día (meta adaptativa + XP). */
class Historial(private val ctx: Context) {
  private val clave = stringPreferencesKey("registro")
  private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

  suspend fun cargar(hoy: Long): Registro {
    val texto = ctx.almacen.data.map { it[clave] }.firstOrNull()
    val r = texto?.let { runCatching { json.decodeFromString(Registro.serializer(), it) }.getOrNull() } ?: Registro(Dia(hoy))
    return cambiarDia(r, hoy)
  }

  suspend fun guardar(r: Registro) {
    val texto = json.encodeToString(Registro.serializer(), r)
    ctx.almacen.edit { it[clave] = texto }
  }

  companion object {
    /** Si cambió el día: cierra ayer en el historial, recalcula la meta y suma la XP. */
    fun cambiarDia(r: Registro, hoy: Long): Registro {
      if (r.hoy.epochDay == hoy) return r
      val historial = (r.historial + r.hoy.pasos).takeLast(7)
      return r.copy(
        hoy = Dia(hoy),
        historial = historial,
        metaAnterior = r.meta,
        meta = com.brasa.app.domain.Reglas.metaAdaptativa(historial, r.meta),
        xpPrevio = r.xpPrevio + com.brasa.app.domain.Reglas.xpPorPasos(r.hoy.pasos),
      )
    }
  }
}
