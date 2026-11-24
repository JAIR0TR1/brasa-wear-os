package com.brasa.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.brasa.app.ui.brasa.Emotion

/**
 * Única fuente de verdad del diseño de Brasa.
 * Concepto: "fuego vivo en la oscuridad". Ningún color, espacio o duración se escribe fuera de aquí.
 */
object Tokens {

  /** Paleta base (tema oscuro). */
  object Colors {
    val Black = Color(0xFF000000)
    val LightFire = Color(0xFFFFF4C2)
    val Fire = Color(0xFFFF8A1F)
    val DeepFire = Color(0xFFD92B12)
    val Magenta = Color(0xFFFF2E88)
    val Violet = Color(0xFF7A1CFF)
    val Gold = Color(0xFFFFD27A)
    val TextPrimary = Color(0xFFFFF3E6)
    val SecondaryText = Color(0xFFB9A89A)
    val Pupil = Color(0xFF2B0D06)
    val Mouth = Color(0xFF3A0A06)
    val Tongue = Color(0xFFFF7A8A)
    val Cheek = Color(0xFFFF6B8B)
    val Water = Color(0xFFBFE3FF)
    val White = Color(0xFFFFFFFF)
    /** "Calma" (respiración) en tema claro: azul profundo, AA sobre el fondo claro. */
    val LightThemeWater = Color(0xFF2B5D8A)

    /** Tema claro: blanco cálido y acentos profundos (AA sobre el fondo). */
    val LightBackground = Color(0xFFFFF8F0)
    val LightText = Color(0xFF2A1408)
    val LightSecondaryText = Color(0xFF6B5444)
    val LightThemeFire = Color(0xFFC2410C)
  }

  /** Colores por emoción: base del cuerpo, núcleo (brillo interior), borde (sombra) y acento de texto. */
  data class EmotionPalette(
    val base: Color,
    val core: Color,
    val edge: Color,
    /** Color de acento legible sobre el fondo del tema (texto, anillos). */
    val accent: Color,
  )

  fun palette(emotion: Emotion, dark: Boolean): EmotionPalette = when (emotion) {
    Emotion.DIM -> EmotionPalette(
      base = Color(0xFF7F93B8), core = Color(0xFFDCE4F2), edge = Color(0xFF3E4C6B),
      accent = if (dark) Color(0xFF9FB2D6) else Color(0xFF41557D),
    )
    Emotion.RESTLESS -> EmotionPalette(
      base = Color(0xFFFF5A4E), core = Color(0xFFFFC4B4), edge = Color(0xFFA3133A),
      accent = if (dark) Color(0xFFFF7A70) else Color(0xFFB8153A),
    )
    Emotion.CALM -> EmotionPalette(
      base = Color(0xFFFF9E45), core = Color(0xFFFFE6BC), edge = Color(0xFFC25A12),
      accent = if (dark) Color(0xFFFFB46E) else Color(0xFFA34C0C),
    )
    Emotion.HAPPY -> EmotionPalette(
      base = Color(0xFFFF8A1F), core = Color(0xFFFFF4C2), edge = Color(0xFFD92B12),
      accent = if (dark) Color(0xFFFFA14D) else Color(0xFFB83A0A),
    )
    Emotion.ECSTATIC -> EmotionPalette(
      base = Color(0xFFFF4F93), core = Color(0xFFFFE7A8), edge = Color(0xFFB0105A),
      accent = if (dark) Color(0xFFFF79AE) else Color(0xFFB0105A),
    )
  }

  /** Espaciado en una escala de 4 dp. */
  object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    /** Área táctil mínima (WCAG / Wear). */
    val touchTarget = 48.dp
    /** Distancia mínima de cualquier trazo o brillo al borde de la pantalla. */
    val safeEdge = 18.dp
  }

  /** Duraciones de movimiento (ms). */
  object Duration {
    const val fast = 140
    const val medium = 320
    const val draw = 900
    const val backgroundTransition = 600
    const val startup = 2000
    const val levelUp = 2600
  }
}
