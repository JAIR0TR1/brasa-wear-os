@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.brasa.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.brasa.app.R

/** Space Grotesk: números y títulos. Fuente variable (eje wght), soportada desde API 26. */
val SpaceGrotesk = FontFamily(
  Font(R.font.space_grotesk, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
  Font(R.font.space_grotesk, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Fraunces itálica: la voz de Brasa (sus frases). */
val Fraunces = FontFamily(
  Font(
    R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic,
    variationSettings = FontVariation.Settings(FontVariation.weight(420), FontVariation.Setting("SOFT", 100f)),
  ),
  Font(
    R.font.fraunces_italic, FontWeight.SemiBold, FontStyle.Italic,
    variationSettings = FontVariation.Settings(FontVariation.weight(600), FontVariation.Setting("SOFT", 100f)),
  ),
)

/** JetBrains Mono: datos pequeños. */
val JetBrainsMono = FontFamily(
  Font(R.font.jetbrains_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
  Font(R.font.jetbrains_mono, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
)

/** Escala tipográfica de Brasa (sp: respeta el tamaño de fuente del sistema). */
object Tipo {
  val cifra = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.02).em)
  val titulo = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = (-0.01).em, textAlign = TextAlign.Center)
  val etiqueta = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.Center)
  val voz = TextStyle(fontFamily = Fraunces, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 17.sp, textAlign = TextAlign.Center)
  val dato = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 0.02.em, textAlign = TextAlign.Center)
  val curvo = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.08.em)
}
