package com.brasa.app.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import com.brasa.app.R
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.componentes.BotonBrasa
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens

/**
 * Antes de pedir permisos, Brasa explica para qué los usa (pedir en contexto) y aclara que no es
 * un dispositivo médico. "Ahora no" deja la app en modo manual, totalmente usable.
 */
@Composable
fun Permisos(onPermitir: () -> Unit, onAhoraNo: () -> Unit, tiempoFijo: Float? = null) {
  val colores = BrasaTema.colores
  val acento = Tokens.paleta(Emocion.FELIZ, colores.oscuro).acento
  FondoEmocion(Emocion.FELIZ, intensidad = 0.8f) {
    Column(
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(Tokens.Espacio.s))
      Brasa(Emocion.FELIZ, Etapa.CHISPA, Modifier.fillMaxWidth(0.5f).height(44.dp), tiempoFijo = tiempoFijo)
      Text(stringResource(R.string.permisos_titulo), style = Tipo.titulo, color = colores.texto)
      Spacer(Modifier.height(Tokens.Espacio.xs))
      Text(stringResource(R.string.permisos_texto), style = Tipo.voz, color = acento)
      Spacer(Modifier.height(Tokens.Espacio.xs))
      // La acción principal, visible sin desplazar.
      BotonBrasa(stringResource(R.string.permitir), acento, onPermitir)
      Box(
        Modifier.defaultMinSize(minHeight = Tokens.Espacio.tactil).fillMaxWidth()
          .clickable(role = Role.Button, onClick = onAhoraNo),
        contentAlignment = Alignment.Center,
      ) {
        Text(stringResource(R.string.ahora_no), style = Tipo.etiqueta, color = colores.textoSecundario)
      }
      Text(stringResource(R.string.aviso_privacidad), style = Tipo.dato, color = colores.textoSecundario)
      Spacer(Modifier.height(Tokens.Espacio.s))
      Text(
        stringResource(R.string.aviso_medico), style = Tipo.dato.copy(textAlign = TextAlign.Center),
        color = colores.textoSecundario,
      )
      Spacer(Modifier.height(36.dp))
    }
  }
}
