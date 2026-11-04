package com.brasa.app.ui.pantallas

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Text
import com.brasa.app.Fuente
import com.brasa.app.R
import com.brasa.app.data.Ajustes
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.brasa.nombreEmocion
import com.brasa.app.ui.brasa.nombreEtapa
import com.brasa.app.ui.componentes.FilaAjuste
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens

/** Acciones de Ajustes (las implementa el ViewModel). */
data class AccionesAjustes(
  val tema: (Boolean) -> Unit,
  val demo: (Boolean) -> Unit,
  val emocion: (Emocion?) -> Unit,
  val etapa: (Etapa?) -> Unit,
  val permisos: () -> Unit = {},
)

private fun <T> siguiente(actual: T?, valores: List<T>): T? {
  val i = if (actual == null) -1 else valores.indexOf(actual)
  return if (i + 1 >= valores.size) null else valores[i + 1]
}

/** Idioma: Sistema → Español → English → Sistema. AppCompat recrea la actividad y lo guarda (autoStoreLocales). */
private fun cambiarIdioma() {
  val actual = AppCompatDelegate.getApplicationLocales().toLanguageTags()
  val nuevo = when {
    actual.isEmpty() -> "es"
    actual.startsWith("es") -> "en"
    else -> ""
  }
  AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(nuevo))
}

@Composable
fun AjustesPantalla(
  ajustes: Ajustes,
  acciones: AccionesAjustes,
  idiomaActual: String = AppCompatDelegate.getApplicationLocales().toLanguageTags(),
  fuente: Fuente = Fuente.SENSORES,
) {
  val colores = BrasaTema.colores
  val acento = colores.fuego
  val estado = rememberScalingLazyListState(initialCenterItemIndex = 0)
  val idioma = when {
    idiomaActual.isEmpty() -> stringResource(R.string.idioma_sistema)
    idiomaActual.startsWith("es") -> "Español"
    else -> "English"
  }
  FondoEmocion(Emocion.TRANQUILA, intensidad = 0.5f) {
    ScalingLazyColumn(
      Modifier.fillMaxSize(),
      state = estado,
      autoCentering = null,
      // Márgenes laterales: las filas no tocan el borde del círculo.
      contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 44.dp),
    ) {
      item {
        Text(stringResource(R.string.ajustes), style = Tipo.titulo, color = colores.texto, modifier = Modifier.padding(bottom = Tokens.Espacio.xs))
      }
      item {
        FilaAjuste(
          stringResource(R.string.ajuste_tema),
          stringResource(if (ajustes.oscuro) R.string.tema_oscuro else R.string.tema_claro),
          acento, { acciones.tema(!ajustes.oscuro) }, Modifier.fillMaxWidth(),
        )
      }
      item { FilaAjuste(stringResource(R.string.ajuste_idioma), idioma, acento, ::cambiarIdioma, Modifier.fillMaxWidth()) }
      item {
        FilaAjuste(
          stringResource(R.string.ajuste_demo),
          stringResource(if (ajustes.demo) R.string.activado else R.string.desactivado),
          acento, { acciones.demo(!ajustes.demo) }, Modifier.fillMaxWidth(),
        )
      }
      if (ajustes.demo) {
        item {
          val e = ajustes.emocionDemo
          FilaAjuste(
            stringResource(R.string.ajuste_emocion),
            if (e == null) stringResource(R.string.automatica) else stringResource(nombreEmocion(e)),
            e?.let { Tokens.paleta(it, colores.oscuro).acento } ?: acento,
            { acciones.emocion(siguiente(e, Emocion.entries)) }, Modifier.fillMaxWidth(),
          )
        }
        item {
          val e = ajustes.etapaDemo
          FilaAjuste(
            stringResource(R.string.ajuste_etapa),
            if (e == null) stringResource(R.string.automatica) else stringResource(nombreEtapa(e)),
            acento, { acciones.etapa(siguiente(e, Etapa.entries)) }, Modifier.fillMaxWidth(),
          )
        }
      }
      item {
        FilaAjuste(
          stringResource(R.string.ajuste_permisos),
          stringResource(
            when (fuente) {
              Fuente.SENSORES -> R.string.permisos_activos
              Fuente.MANUAL -> R.string.permisos_manual
              Fuente.DEMO -> R.string.permisos_demo
            },
          ),
          acento, acciones.permisos, Modifier.fillMaxWidth(),
        )
      }
      item {
        Box(Modifier.padding(top = Tokens.Espacio.s, start = 6.dp, end = 6.dp)) {
          Text(stringResource(R.string.aviso_medico), style = Tipo.voz, color = colores.textoSecundario)
        }
      }
      item {
        Text(stringResource(R.string.aviso_privacidad), style = Tipo.dato, color = colores.textoSecundario, modifier = Modifier.padding(horizontal = 6.dp))
      }
    }
  }
}
