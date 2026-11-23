package com.brasa.app.ui.componentes

import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.basicCurvedText
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material3.Text
import com.brasa.app.ui.theme.BrasaTema
import com.brasa.app.ui.theme.Tipo
import com.brasa.app.ui.theme.Tokens

/**
 * Título superior que sigue el borde: curvo en pantallas redondas, recto y centrado en cuadradas.
 */
@Composable
fun TituloBorde(
  texto: String,
  color: Color = BrasaTema.colores.textoSecundario,
  margen: androidx.compose.ui.unit.Dp = Tokens.Espacio.s,
  /** true: TalkBack lo anuncia como encabezado de la pantalla (false para la hora de Inicio). */
  encabezado: Boolean = true,
) {
  val t = texto.uppercase()
  // TalkBack lee el texto original (no en mayúsculas, que algunas voces deletrean).
  val semantica = Modifier.clearAndSetSemantics {
    contentDescription = texto
    if (encabezado) heading()
  }
  if (LocalConfiguration.current.isScreenRound) {
    CurvedLayout(Modifier.fillMaxSize().then(semantica), anchor = 270f) {
      basicCurvedText(
        t,
        modifier = CurvedModifier.padding(margen),
        style = CurvedTextStyle(
          fontSize = Tipo.curvo.fontSize, fontFamily = Tipo.curvo.fontFamily, fontWeight = Tipo.curvo.fontWeight,
          color = color, letterSpacing = Tipo.curvo.letterSpacing,
        ),
      )
    }
  } else {
    Box(Modifier.fillMaxSize().padding(top = margen), contentAlignment = Alignment.TopCenter) {
      Text(t, style = Tipo.curvo, color = color, maxLines = 1, modifier = semantica)
    }
  }
}
