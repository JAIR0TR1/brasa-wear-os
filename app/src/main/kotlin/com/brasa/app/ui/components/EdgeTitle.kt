package com.brasa.app.ui.components

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
import com.brasa.app.ui.theme.BrasaTheme
import com.brasa.app.ui.theme.BrasaType
import com.brasa.app.ui.theme.Tokens

/**
 * Título superior que sigue el borde: curvo en pantallas redondas, recto y centrado en cuadradas.
 */
@Composable
fun EdgeTitle(
  text: String,
  color: Color = BrasaTheme.colors.secondaryText,
  margin: androidx.compose.ui.unit.Dp = Tokens.Spacing.s,
  /** true: TalkBack lo anuncia como encabezado de la pantalla (false para la hora de Inicio). */
  isHeading: Boolean = true,
) {
  val t = text.uppercase()
  // TalkBack lee el texto original (no en mayúsculas, que algunas voces deletrean).
  val semanticsModifier = Modifier.clearAndSetSemantics {
    contentDescription = text
    if (isHeading) heading()
  }
  if (LocalConfiguration.current.isScreenRound) {
    CurvedLayout(Modifier.fillMaxSize().then(semanticsModifier), anchor = 270f) {
      basicCurvedText(
        t,
        modifier = CurvedModifier.padding(margin),
        style = CurvedTextStyle(
          fontSize = BrasaType.curved.fontSize, fontFamily = BrasaType.curved.fontFamily, fontWeight = BrasaType.curved.fontWeight,
          color = color, letterSpacing = BrasaType.curved.letterSpacing,
        ),
      )
    }
  } else {
    Box(Modifier.fillMaxSize().padding(top = margin), contentAlignment = Alignment.TopCenter) {
      Text(t, style = BrasaType.curved, color = color, maxLines = 1, modifier = semanticsModifier)
    }
  }
}
