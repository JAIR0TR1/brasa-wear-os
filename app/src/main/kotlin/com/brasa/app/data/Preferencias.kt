package com.brasa.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Dos almacenes:
 * - "brasa" (solo en este reloj, excluido de la copia de seguridad): registro del día con pasos y pulso
 *   exactos, historial de pasos por día e id anónimo del reloj.
 * - "brasa_respaldo" (copia de seguridad de Google del usuario, ver res/xml/reglas_extraccion.xml):
 *   ajustes y progreso sin datos de salud (XP total, respiraciones, meta).
 */
private val Context.almacen by preferencesDataStore(name = "brasa")
private val Context.respaldo by preferencesDataStore(name = NOMBRE_RESPALDO)
const val NOMBRE_RESPALDO = "brasa_respaldo"

/** Progreso que viaja a un reloj nuevo (nada de salud: ni pasos, ni pulso, ni historial por día). */
data class Progreso(val xpTotal: Int, val respirosTotales: Int, val meta: Int, val metaAnterior: Int)

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

  /** Ajustes del almacén con copia de seguridad (si faltan, los de versiones anteriores en "brasa"). */
  val ajustes: Flow<Ajustes> = combine(ctx.respaldo.data, ctx.almacen.data) { r, viejo ->
    fun <T> leer(k: Preferences.Key<T>): T? = r[k] ?: viejo[k]
    Ajustes(
      oscuro = leer(K.oscuro) ?: true,
      demo = leer(K.demo) ?: false,
      emocionDemo = leer(K.emocionDemo)?.let { n -> Emocion.entries.firstOrNull { it.name == n } },
      etapaDemo = leer(K.etapaDemo)?.let { n -> Etapa.entries.firstOrNull { it.name == n } },
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

  suspend fun oscuro(v: Boolean) = ctx.respaldo.edit { it[K.oscuro] = v }
  suspend fun demo(v: Boolean) = ctx.respaldo.edit { it[K.demo] = v }
  suspend fun emocionDemo(e: Emocion?) = ctx.respaldo.edit { if (e == null) it.remove(K.emocionDemo) else it[K.emocionDemo] = e.name }
  suspend fun etapaDemo(e: Etapa?) = ctx.respaldo.edit { if (e == null) it.remove(K.etapaDemo) else it[K.etapaDemo] = e.name }
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
  private object R {
    val xp = intPreferencesKey("xp_total")
    val respiros = intPreferencesKey("respiros_totales")
    val meta = intPreferencesKey("meta")
    val metaAnterior = intPreferencesKey("meta_anterior")
  }

  /** Registro de este reloj; si no hay (reloj nuevo o datos borrados), se recupera el progreso de la copia. */
  suspend fun cargar(hoy: Long): Registro {
    val texto = ctx.almacen.data.map { it[clave] }.firstOrNull()
    val r = texto?.let { runCatching { json.decodeFromString(Registro.serializer(), it) }.getOrNull() }
      ?: desdeRespaldo(progresoGuardado(), hoy)
    return cambiarDia(r, hoy)
  }

  suspend fun guardar(r: Registro) {
    val texto = json.encodeToString(Registro.serializer(), r)
    ctx.almacen.edit { it[clave] = texto }
    val p = progresoDe(r)
    ctx.respaldo.edit {
      it[R.xp] = p.xpTotal; it[R.respiros] = p.respirosTotales; it[R.meta] = p.meta; it[R.metaAnterior] = p.metaAnterior
    }
  }

  private suspend fun progresoGuardado(): Progreso? = ctx.respaldo.data.firstOrNull()?.let { p ->
    val xp = p[R.xp] ?: return@let null
    Progreso(xp, p[R.respiros] ?: 0, p[R.meta] ?: com.brasa.app.domain.Reglas.META_INICIAL, p[R.metaAnterior] ?: com.brasa.app.domain.Reglas.META_INICIAL)
  }

  companion object {
    /** Lo que se guarda en la copia: XP total (días anteriores + hoy + respiraciones), respiraciones y meta. */
    fun progresoDe(r: Registro) = Progreso(
      xpTotal = r.xpPrevio + com.brasa.app.domain.Reglas.xpPorPasos(r.hoy.pasos) + r.respirosTotales * com.brasa.app.domain.Reglas.XP_POR_RESPIRACION,
      respirosTotales = r.respirosTotales, meta = r.meta, metaAnterior = r.metaAnterior,
    )

    /**
     * Reloj nuevo: mismo nivel, XP y meta; el día empieza de cero (los pasos de hoy ya están dentro de la XP)
     * y los permisos se vuelven a pedir porque son de cada reloj.
     */
    fun desdeRespaldo(p: Progreso?, hoy: Long): Registro {
      if (p == null) return Registro(Dia(hoy))
      return Registro(
        hoy = Dia(hoy),
        xpPrevio = (p.xpTotal - p.respirosTotales * com.brasa.app.domain.Reglas.XP_POR_RESPIRACION).coerceAtLeast(0),
        respirosTotales = p.respirosTotales, meta = p.meta, metaAnterior = p.metaAnterior,
      )
    }

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
