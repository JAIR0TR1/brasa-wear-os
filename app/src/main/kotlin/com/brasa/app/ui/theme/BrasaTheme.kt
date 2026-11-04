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
data class ColoresBrasa(
  val oscuro: Boolean,
  val fondo: Color,
  val texto: Color,
  val textoSecundario: Color,
  val fuego: Color,
  /** Pista de los anillos (track), muy sutil. */
  val pista: Color,
)

val ColoresOscuros = ColoresBrasa(
  oscuro = true,
  fondo = Tokens.Colores.Negro,
  texto = Tokens.Colores.Texto,
  textoSecundario = Tokens.Colores.TextoSecundario,
  fuego = Tokens.Colores.Fuego,
  pista = Color(0x1FFFF3E6),
)

val ColoresClaros = ColoresBrasa(
  oscuro = false,
  fondo = Tokens.Colores.FondoClaro,
  texto = Tokens.Colores.TextoClaro,
  textoSecundario = Tokens.Colores.TextoSecundarioClaro,
  fuego = Tokens.Colores.FuegoClaroTema,
  pista = Color(0x1A2A1408),
)

val LocalColoresBrasa = staticCompositionLocalOf { ColoresOscuros }

object BrasaTema {
  val colores: ColoresBrasa
    @Composable get() = LocalColoresBrasa.current
}

/** Tema de Brasa: personaliza el MaterialTheme de Wear con los tokens (nunca Material por defecto). */
@Composable
fun BrasaTheme(oscuro: Boolean = true, content: @Composable () -> Unit) {
  val c = if (oscuro) ColoresOscuros else ColoresClaros
  val esquema = ColorScheme(
    primary = c.fuego,
    onPrimary = if (oscuro) Tokens.Colores.Negro else Tokens.Colores.Blanco,
    background = c.fondo,
    onBackground = c.texto,
    onSurface = c.texto,
    onSurfaceVariant = c.textoSecundario,
  )
  CompositionLocalProvider(LocalColoresBrasa provides c) {
    MaterialTheme(colorScheme = esquema, content = content)
  }
}
