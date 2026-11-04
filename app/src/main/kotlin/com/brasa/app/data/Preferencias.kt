package com.brasa.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.almacen by preferencesDataStore(name = "brasa")

/** Ajustes del usuario. El idioma lo guarda AppCompat (autoStoreLocales), no aquí. */
data class Ajustes(
  val oscuro: Boolean = true,
  val demo: Boolean = true,
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
      demo = p[K.demo] ?: true,
      emocionDemo = p[K.emocionDemo]?.let { n -> Emocion.entries.firstOrNull { it.name == n } },
      etapaDemo = p[K.etapaDemo]?.let { n -> Etapa.entries.firstOrNull { it.name == n } },
    )
  }

  suspend fun oscuro(v: Boolean) = ctx.almacen.edit { it[K.oscuro] = v }
  suspend fun demo(v: Boolean) = ctx.almacen.edit { it[K.demo] = v }
  suspend fun emocionDemo(e: Emocion?) = ctx.almacen.edit { if (e == null) it.remove(K.emocionDemo) else it[K.emocionDemo] = e.name }
  suspend fun etapaDemo(e: Etapa?) = ctx.almacen.edit { if (e == null) it.remove(K.etapaDemo) else it[K.etapaDemo] = e.name }
}
