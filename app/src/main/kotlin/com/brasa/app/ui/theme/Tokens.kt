package com.brasa.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.brasa.app.ui.brasa.Emocion

/**
 * Única fuente de verdad del diseño de Brasa.
 * Concepto: "fuego vivo en la oscuridad". Ningún color, espacio o duración se escribe fuera de aquí.
 */
object Tokens {

  /** Paleta base (tema oscuro). */
  object Colores {
    val Negro = Color(0xFF000000)
    val FuegoClaro = Color(0xFFFFF4C2)
    val Fuego = Color(0xFFFF8A1F)
    val FuegoProfundo = Color(0xFFD92B12)
    val Magenta = Color(0xFFFF2E88)
    val Violeta = Color(0xFF7A1CFF)
    val Dorado = Color(0xFFFFD27A)
    val Texto = Color(0xFFFFF3E6)
    val TextoSecundario = Color(0xFFB9A89A)
    val Pupila = Color(0xFF2B0D06)
    val Boca = Color(0xFF3A0A06)
    val Lengua = Color(0xFFFF7A8A)
    val Mejilla = Color(0xFFFF6B8B)
    val Agua = Color(0xFFBFE3FF)
    val Blanco = Color(0xFFFFFFFF)
    /** "Calma" (respiración) en tema claro: azul profundo, AA sobre el fondo claro. */
    val AguaClaroTema = Color(0xFF2B5D8A)

    /** Tema claro: blanco cálido y acentos profundos (AA sobre el fondo). */
    val FondoClaro = Color(0xFFFFF8F0)
    val TextoClaro = Color(0xFF2A1408)
    val TextoSecundarioClaro = Color(0xFF6B5444)
    val FuegoClaroTema = Color(0xFFC2410C)
  }

  /** Colores por emoción: base del cuerpo, núcleo (brillo interior), borde (sombra) y acento de texto. */
  data class PaletaEmocion(
    val base: Color,
    val nucleo: Color,
    val borde: Color,
    /** Color de acento legible sobre el fondo del tema (texto, anillos). */
    val acento: Color,
  )

  fun paleta(emocion: Emocion, oscuro: Boolean): PaletaEmocion = when (emocion) {
    Emocion.APAGADA -> PaletaEmocion(
      base = Color(0xFF7F93B8), nucleo = Color(0xFFDCE4F2), borde = Color(0xFF3E4C6B),
      acento = if (oscuro) Color(0xFF9FB2D6) else Color(0xFF41557D),
    )
    Emocion.INQUIETA -> PaletaEmocion(
      base = Color(0xFFFF5A4E), nucleo = Color(0xFFFFC4B4), borde = Color(0xFFA3133A),
      acento = if (oscuro) Color(0xFFFF7A70) else Color(0xFFB8153A),
    )
    Emocion.TRANQUILA -> PaletaEmocion(
      base = Color(0xFFFF9E45), nucleo = Color(0xFFFFE6BC), borde = Color(0xFFC25A12),
      acento = if (oscuro) Color(0xFFFFB46E) else Color(0xFFA34C0C),
    )
    Emocion.FELIZ -> PaletaEmocion(
      base = Color(0xFFFF8A1F), nucleo = Color(0xFFFFF4C2), borde = Color(0xFFD92B12),
      acento = if (oscuro) Color(0xFFFFA14D) else Color(0xFFB83A0A),
    )
    Emocion.EUFORICA -> PaletaEmocion(
      base = Color(0xFFFF4F93), nucleo = Color(0xFFFFE7A8), borde = Color(0xFFB0105A),
      acento = if (oscuro) Color(0xFFFF79AE) else Color(0xFFB0105A),
    )
  }

  /** Espaciado en una escala de 4 dp. */
  object Espacio {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    /** Área táctil mínima (WCAG / Wear). */
    val tactil = 48.dp
    /** Distancia mínima de cualquier trazo o brillo al borde de la pantalla. */
    val bordeSeguro = 18.dp
  }

  /** Duraciones de movimiento (ms). */
  object Duracion {
    const val rapida = 140
    const val media = 320
    const val dibujo = 900
    const val transicionFondo = 600
    const val arranque = 2000
  }
}
