package com.brasa.app

import com.brasa.app.domain.Muestra
import com.brasa.app.domain.Reglas
import com.brasa.app.ui.brasa.Emocion
import com.brasa.app.ui.brasa.Etapa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasTest {

  @Test fun emocionSegunProgreso() {
    assertEquals(Emocion.APAGADA, Reglas.emocion(0.1f, false, 0))
    assertEquals(Emocion.TRANQUILA, Reglas.emocion(0.4f, false, 0))
    assertEquals(Emocion.FELIZ, Reglas.emocion(0.75f, false, 0))
    assertEquals(Emocion.EUFORICA, Reglas.emocion(1.2f, false, 0))
  }

  @Test fun tensionTienePrioridadEInactividadApaga() {
    assertEquals(Emocion.INQUIETA, Reglas.emocion(1.5f, true, 0))
    assertEquals(Emocion.APAGADA, Reglas.emocion(0.8f, false, 200))
  }

  @Test fun nivelesYEtapas() {
    assertEquals(1, Reglas.nivel(0))
    assertEquals(5, Reglas.nivel(400))
    assertEquals(Etapa.LLAMA, Reglas.etapa(400))
    assertEquals(Etapa.FOGATA, Reglas.etapa(2025))
    assertEquals(Etapa.LEYENDA, Reglas.etapa(9025))
    assertEquals(85, Reglas.xpPorPasos(8500))
  }

  @Test fun pulsoAltoEnReposoDisparaTension() {
    val ahora = 10_000_000L
    val muestras = listOf(Muestra(ahora - 120_000, 92), Muestra(ahora - 60_000, 95), Muestra(ahora, 97))
    assertTrue(Reglas.pulsoElevadoEnReposo(muestras, base = 65, ultimoMovimientoMs = ahora - 10 * 60_000, ahoraMs = ahora))
  }

  @Test fun pulsoAltoMoviendoseNoEsTension() {
    val ahora = 10_000_000L
    val muestras = listOf(Muestra(ahora - 60_000, 120), Muestra(ahora, 125))
    assertFalse(Reglas.pulsoElevadoEnReposo(muestras, base = 65, ultimoMovimientoMs = ahora - 60_000, ahoraMs = ahora))
  }

  @Test fun pulsoNormalNoEsTension() {
    val ahora = 10_000_000L
    val muestras = listOf(Muestra(ahora - 60_000, 70), Muestra(ahora, 72))
    assertFalse(Reglas.pulsoElevadoEnReposo(muestras, base = 65, ultimoMovimientoMs = 0, ahoraMs = ahora))
  }

  @Test fun baseReposoUsaPercentil20() {
    assertEquals(65, Reglas.baseReposo(listOf(80, 90)))
    assertEquals(62, Reglas.baseReposo(listOf(60, 61, 62, 64, 66, 70, 80, 90, 100, 110)))
  }

  @Test fun metaAdaptativaSubeConLimite() {
    // Media 10.000 × 1,05 = 10.500, pero la meta actual es 8.000 → máximo +15 % = 9.200.
    assertEquals(9200, Reglas.metaAdaptativa(List(7) { 10_000 }, 8000))
    // Media 8.000 × 1,05 = 8.400 (dentro del límite).
    assertEquals(8400, Reglas.metaAdaptativa(List(7) { 8_000 }, 8000))
    // Sin historial se mantiene; nunca baja de 3.000.
    assertEquals(8000, Reglas.metaAdaptativa(emptyList(), 8000))
    assertEquals(3000, Reglas.metaAdaptativa(List(7) { 500 }, 3200))
  }

  @Test fun rachaCuentaDiasActivosRecientes() {
    assertEquals(2, Reglas.racha(listOf(9000, 1000, 6000, 7000), 8000))
    assertEquals(0, Reglas.racha(listOf(9000, 100), 8000))
  }
}
