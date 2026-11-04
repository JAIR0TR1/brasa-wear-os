package com.brasa.app.ui.brasa

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.brasa.app.ui.theme.Tokens

enum class Emocion { APAGADA, INQUIETA, TRANQUILA, FELIZ, EUFORICA }

/** Etapas de evolución por nivel. */
enum class Etapa(val nivelMinimo: Int) {
  CHISPA(1), LLAMA(5), FOGATA(10), LEYENDA(20);

  companion object {
    fun deNivel(nivel: Int): Etapa = entries.last { nivel >= it.nivelMinimo }
  }
}

/**
 * Parámetros continuos del personaje. Cambiar de emoción = interpolar de una pose a otra,
 * así toda transición queda animada sin dibujos separados.
 * Ángulos de brazos en grados desde "colgando": positivo = hacia fuera/arriba, negativo = hacia dentro.
 */
@Immutable
data class Pose(
  val base: Color,
  val nucleo: Color,
  val borde: Color,
  val escala: Float,
  val ojoAbierto: Float,
  val ojoFeliz: Float,
  val ojoCaido: Float,
  val ceja: Float,
  val cejaVisible: Float,
  val boca: Float,
  val bocaAbierta: Float,
  val bocaTemblor: Float,
  val mejillas: Float,
  val brazoIzq: Float,
  val brazoDer: Float,
  val saludo: Float,
  val balanceo: Float,
  val flotaAmp: Float,
  val flotaVel: Float,
  val salto: Float,
  val temblor: Float,
  val avivar: Float,
  val lagrima: Float,
  val sudor: Float,
  val chispas: Float,
  val brillo: Float,
)

/** Forma por etapa de evolución (se interpola igual que la pose). */
@Immutable
data class Forma(
  val escala: Float,
  val punta: Float,
  val extremidades: Float,
  val laterales: Float,
  val corona: Float,
  val orbita: Float,
)

fun formaDe(etapa: Etapa): Forma = when (etapa) {
  Etapa.CHISPA -> Forma(escala = 0.74f, punta = 0.22f, extremidades = 0.82f, laterales = 0f, corona = 0f, orbita = 0f)
  Etapa.LLAMA -> Forma(escala = 0.9f, punta = 1f, extremidades = 1f, laterales = 0f, corona = 0f, orbita = 0f)
  Etapa.FOGATA -> Forma(escala = 0.98f, punta = 1.08f, extremidades = 1f, laterales = 1f, corona = 0f, orbita = 0f)
  Etapa.LEYENDA -> Forma(escala = 0.96f, punta = 1.12f, extremidades = 1f, laterales = 1f, corona = 1f, orbita = 1f)
}

fun poseDe(emocion: Emocion): Pose {
  val p = Tokens.paleta(emocion, oscuro = true)
  return when (emocion) {
    Emocion.APAGADA -> Pose(
      p.base, p.nucleo, p.borde, escala = 0.84f,
      ojoAbierto = 0.85f, ojoFeliz = 0f, ojoCaido = 0.62f, ceja = 0.7f, cejaVisible = 0.9f,
      boca = -0.75f, bocaAbierta = 0f, bocaTemblor = 0f, mejillas = 0f,
      brazoIzq = 6f, brazoDer = 6f, saludo = 0f, balanceo = 3f,
      flotaAmp = 1.2f, flotaVel = 0.9f, salto = 0f, temblor = 0f, avivar = 0.25f,
      lagrima = 1f, sudor = 0f, chispas = 0f, brillo = 0.35f,
    )
    Emocion.INQUIETA -> Pose(
      p.base, p.nucleo, p.borde, escala = 0.96f,
      ojoAbierto = 1.32f, ojoFeliz = 0f, ojoCaido = 0f, ceja = 1f, cejaVisible = 1f,
      boca = 0f, bocaAbierta = 0f, bocaTemblor = 1f, mejillas = 0f,
      brazoIzq = -78f, brazoDer = -78f, saludo = 0f, balanceo = 4f,
      flotaAmp = 1.6f, flotaVel = 2.6f, salto = 0f, temblor = 1f, avivar = 1.4f,
      lagrima = 0f, sudor = 1f, chispas = 0f, brillo = 0.75f,
    )
    Emocion.TRANQUILA -> Pose(
      p.base, p.nucleo, p.borde, escala = 0.95f,
      ojoAbierto = 1f, ojoFeliz = 0f, ojoCaido = 0f, ceja = 0f, cejaVisible = 0f,
      boca = 0.55f, bocaAbierta = 0f, bocaTemblor = 0f, mejillas = 0.35f,
      brazoIzq = 26f, brazoDer = 26f, saludo = 0f, balanceo = 7f,
      flotaAmp = 2.2f, flotaVel = 1.3f, salto = 0f, temblor = 0f, avivar = 0.7f,
      lagrima = 0f, sudor = 0f, chispas = 0f, brillo = 0.7f,
    )
    Emocion.FELIZ -> Pose(
      p.base, p.nucleo, p.borde, escala = 1f,
      ojoAbierto = 1.22f, ojoFeliz = 0f, ojoCaido = 0f, ceja = 0f, cejaVisible = 0f,
      boca = 1f, bocaAbierta = 0.75f, bocaTemblor = 0f, mejillas = 1f,
      brazoIzq = 34f, brazoDer = 148f, saludo = 1f, balanceo = 5f,
      flotaAmp = 2.6f, flotaVel = 1.9f, salto = 0f, temblor = 0f, avivar = 1f,
      lagrima = 0f, sudor = 0f, chispas = 0f, brillo = 0.9f,
    )
    Emocion.EUFORICA -> Pose(
      p.base, p.nucleo, p.borde, escala = 1.04f,
      ojoAbierto = 1f, ojoFeliz = 1f, ojoCaido = 0f, ceja = 0f, cejaVisible = 0f,
      boca = 1f, bocaAbierta = 1f, bocaTemblor = 0f, mejillas = 1f,
      brazoIzq = 158f, brazoDer = 158f, saludo = 0f, balanceo = 12f,
      flotaAmp = 1f, flotaVel = 3.2f, salto = 1f, temblor = 0f, avivar = 1.5f,
      lagrima = 0f, sudor = 0f, chispas = 1f, brillo = 1f,
    )
  }
}

private fun l(a: Float, b: Float, t: Float) = a + (b - a) * t

fun lerpPose(a: Pose, b: Pose, t: Float) = Pose(
  base = lerp(a.base, b.base, t), nucleo = lerp(a.nucleo, b.nucleo, t), borde = lerp(a.borde, b.borde, t),
  escala = l(a.escala, b.escala, t), ojoAbierto = l(a.ojoAbierto, b.ojoAbierto, t), ojoFeliz = l(a.ojoFeliz, b.ojoFeliz, t),
  ojoCaido = l(a.ojoCaido, b.ojoCaido, t), ceja = l(a.ceja, b.ceja, t), cejaVisible = l(a.cejaVisible, b.cejaVisible, t),
  boca = l(a.boca, b.boca, t), bocaAbierta = l(a.bocaAbierta, b.bocaAbierta, t), bocaTemblor = l(a.bocaTemblor, b.bocaTemblor, t),
  mejillas = l(a.mejillas, b.mejillas, t), brazoIzq = l(a.brazoIzq, b.brazoIzq, t), brazoDer = l(a.brazoDer, b.brazoDer, t),
  saludo = l(a.saludo, b.saludo, t), balanceo = l(a.balanceo, b.balanceo, t), flotaAmp = l(a.flotaAmp, b.flotaAmp, t),
  flotaVel = l(a.flotaVel, b.flotaVel, t), salto = l(a.salto, b.salto, t), temblor = l(a.temblor, b.temblor, t),
  avivar = l(a.avivar, b.avivar, t), lagrima = l(a.lagrima, b.lagrima, t), sudor = l(a.sudor, b.sudor, t),
  chispas = l(a.chispas, b.chispas, t), brillo = l(a.brillo, b.brillo, t),
)

fun lerpForma(a: Forma, b: Forma, t: Float) = Forma(
  escala = l(a.escala, b.escala, t), punta = l(a.punta, b.punta, t), extremidades = l(a.extremidades, b.extremidades, t),
  laterales = l(a.laterales, b.laterales, t), corona = l(a.corona, b.corona, t), orbita = l(a.orbita, b.orbita, t),
)
