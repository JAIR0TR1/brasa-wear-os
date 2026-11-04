package com.brasa.app.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.brasa.app.EstadoBrasa
import com.brasa.app.R
import com.brasa.app.domain.Reglas
import com.brasa.app.ui.brasa.Brasa
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.brasa.FondoEmocion
import com.brasa.app.ui.brasa.nombreEtapa
import com.brasa.app.ui.componentes.AnilloBorde
import com.brasa.app.ui.componentes.TituloBorde
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens

/**
 * Evolución: etapa actual, nivel, XP (anillo dorado = progreso al siguiente nivel) y racha de días activos.
 */
@Composable
fun Evolucion(estado: EstadoBrasa, tiempoFijo: Float? = null) {
  val colores = BrasaTema.colores
  val oro = if (colores.oscuro) Tokens.Colores.Dorado else Tokens.Colores.FuegoClaroTema
  val siguiente = Etapa.entries.firstOrNull { it.nivelMinimo > estado.nivel }
  val faltan = siguiente?.let { Reglas.xpParaNivel(it.nivelMinimo) - estado.xp } ?: 0
  val desc = stringResource(R.string.evolucion_desc, stringResource(nombreEtapa(estado.etapa)), estado.nivel, estado.xp, estado.racha)

  FondoEmocion(estado.emocion, intensidad = 0.8f) {
    AnilloBorde(estado.progresoNivel, oro, animado = tiempoFijo == null)
    TituloBorde(stringResource(R.string.evolucion), margen = 18.dp)
    Column(
      Modifier.fillMaxSize().padding(horizontal = 28.dp).semantics(mergeDescendants = true) { contentDescription = desc },
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(Modifier.height(34.dp))
      Brasa(estado.emocion, estado.etapa, Modifier.fillMaxWidth().weight(1f), tiempoFijo = tiempoFijo)
      Text(stringResource(nombreEtapa(estado.etapa)), style = Tipo.titulo, color = colores.texto, maxLines = 1)
      Text(
        stringResource(R.string.nivel_xp, estado.nivel, java.text.NumberFormat.getIntegerInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(estado.xp)),
        style = Tipo.dato, color = oro, maxLines = 1,
      )
      Spacer(Modifier.height(6.dp))
      // Racha: siete brasas; las encendidas son días activos seguidos.
      Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 7) {
          val encendida = i < estado.racha.coerceAtMost(7)
          Canvas(Modifier.size(10.dp)) {
            val c = center
            if (encendida) {
              drawCircle(Brush.radialGradient(listOf(Tokens.Colores.FuegoClaro, Tokens.Colores.Fuego, Color.Transparent), center = c, radius = size.minDimension), size.minDimension, c)
              drawCircle(Tokens.Colores.FuegoClaro, size.minDimension * 0.24f, c)
            } else {
              drawCircle(colores.pista, size.minDimension * 0.28f, c)
            }
          }
          if (i < 6) Spacer(Modifier.width(3.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.racha_dias, estado.racha), style = Tipo.dato, color = colores.textoSecundario)
      }
      Spacer(Modifier.height(2.dp))
      Text(
        if (siguiente != null) stringResource(R.string.siguiente_etapa, stringResource(nombreEtapa(siguiente)), siguiente.nivelMinimo, java.text.NumberFormat.getIntegerInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0]).format(faltan.coerceAtLeast(0)))
        else stringResource(R.string.etapa_maxima),
        style = Tipo.voz.copy(fontSize = 12.sp, lineHeight = 14.sp), color = colores.textoSecundario, maxLines = 2,
        // Abajo el círculo se estrecha: margen extra para que no se corte.
        modifier = Modifier.padding(horizontal = 10.dp),
      )
      Spacer(Modifier.height(28.dp))
    }
  }
}
