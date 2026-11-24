package com.brasa.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** Colores semánticos del tema activo. */
@Immutable
data class BrasaColors(
  val dark: Boolean,
  val background: Color,
  val text: Color,
  val secondaryText: Color,
  val fire: Color,
  /** Pista de los anillos (track), muy sutil. */
  val track: Color,
)

val DarkColors = BrasaColors(
  dark = true,
  background = Tokens.Colors.Black,
  text = Tokens.Colors.TextPrimary,
  secondaryText = Tokens.Colors.SecondaryText,
  fire = Tokens.Colors.Fire,
  track = Color(0x1FFFF3E6),
)

val LightColors = BrasaColors(
  dark = false,
  background = Tokens.Colors.LightBackground,
  text = Tokens.Colors.LightText,
  secondaryText = Tokens.Colors.LightSecondaryText,
  fire = Tokens.Colors.LightThemeFire,
  track = Color(0x1A2A1408),
)

val LocalBrasaColors = staticCompositionLocalOf { DarkColors }

object BrasaTheme {
  val colors: BrasaColors
    @Composable get() = LocalBrasaColors.current
}

/** Tema de Brasa: personaliza el MaterialTheme de Wear con los tokens (nunca Material por defecto). */
@Composable
fun BrasaTheme(dark: Boolean = true, content: @Composable () -> Unit) {
  val c = if (dark) DarkColors else LightColors
  val scheme = ColorScheme(
    primary = c.fire,
    onPrimary = if (dark) Tokens.Colors.Black else Tokens.Colors.White,
    background = c.background,
    onBackground = c.text,
    onSurface = c.text,
    onSurfaceVariant = c.secondaryText,
  )
  CompositionLocalProvider(LocalBrasaColors provides c) {
    MaterialTheme(colorScheme = scheme, content = content)
  }
}
