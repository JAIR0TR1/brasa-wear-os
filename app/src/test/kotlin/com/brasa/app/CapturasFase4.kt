package com.brasa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.brasa.app.data.Ajustes
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import com.brasa.app.ui.pantallas.Evolucion
import com.brasa.app.ui.pantallas.GraficaPasos
import com.brasa.app.ui.pantallas.GraficaPulso
import com.brasa.app.ui.pantallas.GraficaSemana
import com.brasa.app.ui.pantallas.IndicadorPaginas
import com.brasa.app.ui.theme.BrasaTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

val pantallasFase4 = listOf(
  "grafica-pasos", "grafica-pulso", "grafica-semana", "grafica-vacia-pasos", "grafica-vacia-pulso",
  "evolucion-chispa", "evolucion-llama", "evolucion-fogata", "evolucion-leyenda",
  "grafica-midiendo-pulso", "grafica-semana-primer-dia",
)

/** Fecha y hora fijas: las capturas no pueden depender del día en que se ejecutan. */
val HOY_CAPTURAS: java.time.LocalDate = java.time.LocalDate.of(2026, 11, 4)
/** 18:30: la hora que se ve en Inicio y el límite de los datos demo de las gráficas coinciden. */
const val MINUTO_CAPTURAS = 18 * 60 + 30
const val HORA_CAPTURAS = MINUTO_CAPTURAS / 60
const val HORA_TEXTO_CAPTURAS = "18:30"

@Composable
fun ContenidoFase4(nombre: String) {
  val demo = estadoDemo(Ajustes(demo = true), 0, false, MINUTO_CAPTURAS)
  val vacio = demo.copy(pasos = 0, pasosPorHora = List(24) { 0 }, pulsos = emptyList(), respiros = emptyList())
  val pagina = when {
    nombre.contains("pasos") -> 1; nombre.contains("pulso") -> 2; nombre.contains("semana") -> 3; else -> 4
  }
  Box(Modifier.fillMaxSize()) {
    when (nombre) {
      "grafica-pasos" -> GraficaPasos(demo.copy(pasosPorHora = demoPasosPorHora(demo.pasos, HORA_CAPTURAS)), horaActual = HORA_CAPTURAS, fijo = true)
      "grafica-pulso" -> GraficaPulso(demo, fijo = true)
      "grafica-semana" -> GraficaSemana(demo, fijo = true, hoy = HOY_CAPTURAS)
      "grafica-semana-primer-dia" -> GraficaSemana(demo.copy(historial = emptyList(), pasos = 2300, meta = 6000, metaAnterior = 6000), fijo = true, hoy = HOY_CAPTURAS)
      "grafica-midiendo-pulso" -> GraficaPulso(demo.copy(pulsos = demo.pulsos.takeLast(2), respiros = emptyList()), fijo = true)
      "grafica-vacia-pasos" -> GraficaPasos(vacio, horaActual = 9, fijo = true)
      "grafica-vacia-pulso" -> GraficaPulso(vacio, fijo = true)
      else -> {
        val etapa = Etapa.valueOf(nombre.substringAfter("evolucion-").uppercase())
        Evolucion(estadoDemo(Ajustes(demo = true, etapaDemo = etapa, emocionDemo = Emocion.FELIZ), 0, false, MINUTO_CAPTURAS), tiempoFijo = 1.3f)
      }
    }
    IndicadorPaginas(total = 6, actual = pagina)
  }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CapturasFase4(private val pantalla: Int, private val variante: Variante) {

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}")
    fun casos(): List<Array<Any>> {
      val todas = pantallasFase4.indices.map { arrayOf<Any>(it, redondoGrande) }
      val clave = listOf("grafica-pasos", "grafica-pulso", "grafica-semana", "evolucion-fogata", "grafica-midiendo-pulso", "grafica-semana-primer-dia").map { pantallasFase4.indexOf(it) }
      return todas + clave.flatMap { p -> variantes.drop(1).map { arrayOf<Any>(p, it) } }
    }
  }

  @get:Rule val compose = createComposeRule()

  @Test
  fun captura() {
    RuntimeEnvironment.setQualifiers(variante.calificadores)
    RuntimeEnvironment.setFontScale(variante.escalaFuente)
    val nombre = pantallasFase4[pantalla]
    compose.setContent {
      BrasaTheme(oscuro = variante.oscuro) {
        Box(
          Modifier.fillMaxSize().background(Color(0xFF141414))
            .then(if (variante.redondo) Modifier.clip(CircleShape) else Modifier),
        ) { ContenidoFase4(nombre) }
      }
    }
    compose.onRoot().captureRoboImage("../docs/capturas/roborazzi/fase4/${variante.nombre}/$nombre.png")
  }
}
