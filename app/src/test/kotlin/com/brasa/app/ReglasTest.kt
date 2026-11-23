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

  @Test fun emocionEnLosLimites() {
    assertEquals(Emocion.APAGADA, Reglas.emocion(0.199f, false, 0))
    assertEquals(Emocion.TRANQUILA, Reglas.emocion(0.2f, false, 0))
    assertEquals(Emocion.FELIZ, Reglas.emocion(0.6f, false, 0))
    assertEquals(Emocion.EUFORICA, Reglas.emocion(1f, false, 0))
    // Meta cumplida pesa más que la inactividad; 179 min quieta aún no apaga.
    assertEquals(Emocion.EUFORICA, Reglas.emocion(1f, false, 500))
    assertEquals(Emocion.FELIZ, Reglas.emocion(0.7f, false, Reglas.MINUTOS_QUIETO_APAGADA - 1))
  }

  @Test fun xpYProgresoDentroDelNivel() {
    assertEquals(0, Reglas.xpParaNivel(1))
    assertEquals(400, Reglas.xpParaNivel(5))
    assertEquals(0f, Reglas.progresoNivel(400))
    assertEquals(0.5f, Reglas.progresoNivel(400 + (625 - 400) / 2), 0.01f)
    assertEquals(1, Reglas.nivel(-50)) // nunca por debajo de 1
    assertEquals(0, Reglas.xpPorPasos(99))
  }

  @Test fun metaAdaptativaBajaConLimite() {
    // Media 4.000 × 1,05 = 4.200, pero desde 8.000 solo puede bajar un 15 % → 6.800.
    assertEquals(6800, Reglas.metaAdaptativa(List(7) { 4_000 }, 8000))
    // Los días sin datos (0) no cuentan; nunca pasa de 15.000.
    assertEquals(15000, Reglas.metaAdaptativa(listOf(0, 0, 40_000), 14_000))
  }

  @Test fun tensionNecesitaDosLecturasRecientes() {
    val ahora = 10_000_000L
    // Una sola lectura alta, o lecturas altas pero viejas (> 3 min), no bastan.
    assertFalse(Reglas.pulsoElevadoEnReposo(listOf(Muestra(ahora, 120)), 65, 0, ahora))
    assertFalse(Reglas.pulsoElevadoEnReposo(listOf(Muestra(ahora - 400_000, 120), Muestra(ahora - 300_000, 120)), 65, 0, ahora))
    // Justo en el umbral (base + 20) sí cuenta.
    assertTrue(Reglas.pulsoElevadoEnReposo(listOf(Muestra(ahora - 60_000, 85), Muestra(ahora, 85)), 65, 0, ahora))
  }

  @Test fun metaSeCelebraUnaSolaVezAlCruzarEl100() {
    assertTrue(Reglas.cumplioMeta(0.98f, 1.0f))
    assertFalse(Reglas.cumplioMeta(1.0f, 1.2f)) // ya estaba cumplida
    assertFalse(Reglas.cumplioMeta(0.5f, 0.9f))
  }

  @Test fun subidaDeNivelYEtapaNueva() {
    assertTrue(Reglas.subioDeNivel(6, 7))
    assertFalse(Reglas.subioDeNivel(7, 7))
    assertFalse(Reglas.subioDeNivel(8, 7))
    assertEquals(null, Reglas.etapaNueva(6, 7))
    assertEquals(Etapa.FOGATA, Reglas.etapaNueva(9, 10))
    assertEquals(Etapa.LEYENDA, Reglas.etapaNueva(12, 22))
    assertEquals(null, Reglas.etapaNueva(12, 9)) // bajar no es una etapa nueva
  }

  @Test fun nivelesAgregadosQueSalenDelReloj() {
    assertEquals(com.brasa.app.data.NivelActividad.BAJO, com.brasa.app.data.nivelDe(0.29f))
    assertEquals(com.brasa.app.data.NivelActividad.MEDIO, com.brasa.app.data.nivelDe(0.3f))
    assertEquals(com.brasa.app.data.NivelActividad.ALTO, com.brasa.app.data.nivelDe(0.8f))
    assertEquals(com.brasa.app.data.NivelActividad.ALTO, com.brasa.app.data.nivelDe(3f))
  }
}

class HistorialTest {
  @Test fun cambioDeDiaCierraElDiaYRecalculaMeta() {
    val r = com.brasa.app.data.Registro(com.brasa.app.data.Dia(epochDay = 100, pasos = 9000), historial = List(6) { 8000 }, meta = 8000)
    val n = com.brasa.app.data.Historial.cambiarDia(r, 101)
    assertEquals(101, n.hoy.epochDay)
    assertEquals(0, n.hoy.pasos)
    assertEquals(9000, n.historial.last())
    assertEquals(8000, n.metaAnterior)
    assertEquals(8600, n.meta) // media 8143 × 1,05 = 8550 → 8600
    assertEquals(90, n.xpPrevio)
  }

  @Test fun respaldoConservaNivelYMetaSinDatosDeSalud() {
    val r = com.brasa.app.data.Registro(
      com.brasa.app.data.Dia(epochDay = 100, pasos = 6000, pulsos = listOf(com.brasa.app.domain.Muestra(1L, 70))),
      historial = List(6) { 8000 }, meta = 8200, metaAnterior = 8000, xpPrevio = 1500, respirosTotales = 3, permisosPreguntados = true,
    )
    val p = com.brasa.app.data.Historial.progresoDe(r)
    assertEquals(1500 + 60 + 30, p.xpTotal)
    val nuevo = com.brasa.app.data.Historial.desdeRespaldo(p, 105)
    // Mismo nivel y XP en el reloj nuevo.
    val xpNuevo = nuevo.xpPrevio + Reglas.xpPorPasos(nuevo.hoy.pasos) + nuevo.respirosTotales * Reglas.XP_POR_RESPIRACION
    assertEquals(p.xpTotal, xpNuevo)
    assertEquals(Reglas.nivel(p.xpTotal), Reglas.nivel(xpNuevo))
    assertEquals(8200, nuevo.meta)
    // Nada de salud ni permisos: el día empieza vacío y se vuelven a pedir los permisos.
    assertEquals(0, nuevo.hoy.pasos)
    assertTrue(nuevo.hoy.pulsos.isEmpty())
    assertTrue(nuevo.historial.isEmpty())
    assertFalse(nuevo.permisosPreguntados)
  }

  @Test fun sinRespaldoEmpiezaDeCero() {
    val r = com.brasa.app.data.Historial.desdeRespaldo(null, 105)
    assertEquals(0, r.xpPrevio)
    assertEquals(Reglas.META_INICIAL, r.meta)
  }

  @Test fun mismoDiaNoCambia() {
    val r = com.brasa.app.data.Registro(com.brasa.app.data.Dia(epochDay = 100, pasos = 500))
    assertEquals(r, com.brasa.app.data.Historial.cambiarDia(r, 100))
  }
}

class DemoTest {
  @Test fun demoNuncaPintaHorasFuturasYCuadraElTotal() {
    for (hora in 0..23) {
      val porHora = demoPasosPorHora(6200, hora)
      org.junit.Assert.assertTrue("hora $hora", porHora.drop(hora + 1).all { it == 0 })
      org.junit.Assert.assertTrue("hora $hora: total ${porHora.sum()}", porHora.sum() in 6200 - 24..6200)
    }
  }

  @Test fun pulsoDemoLlegaSoloHastaAhora() {
    val inicio = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    val p = demoPulsos(10 * 60)
    assertEquals(10 * 60 * 60_000L, p.last().ms - inicio)
    assertTrue(demoRespiros(10 * 60).isEmpty()) // la respiración de las 16:25 aún no ha pasado
    assertEquals(1, demoRespiros(20 * 60).size)
  }
}
