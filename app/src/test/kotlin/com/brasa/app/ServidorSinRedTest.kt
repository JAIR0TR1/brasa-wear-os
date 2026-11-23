package com.brasa.app

import com.brasa.app.data.ClienteServidor
import com.brasa.app.data.NivelActividad
import com.brasa.app.data.Situacion
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.net.ServerSocket

/**
 * La app debe funcionar SIEMPRE sin servidor: el cliente nunca lanza ni se queda colgado;
 * devuelve null y la interfaz usa sus frases locales.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ServidorSinRedTest {
  private var servidor: HttpServer? = null

  @After fun cerrar() { servidor?.stop(0) }

  /** Servidor HTTP local que responde siempre [estado] con [cuerpo] tras [esperaMs]. */
  private fun servidorFalso(estado: Int, cuerpo: String, esperaMs: Long = 0): String {
    val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    s.createContext("/") { ex ->
      Thread.sleep(esperaMs)
      val b = cuerpo.toByteArray()
      ex.sendResponseHeaders(estado, b.size.toLong())
      ex.responseBody.use { it.write(b) }
    }
    s.start()
    servidor = s
    return "http://127.0.0.1:${s.address.port}"
  }

  private fun pedir(c: ClienteServidor) = runBlocking {
    c.mensaje("3f6c2a1e-9b4d-4c7a-8e21-5d0b9a7f1c33", "es", Situacion.TENSION, NivelActividad.MEDIO, "inquieta", "llama")
  }

  /** Tiempo de una llamada en ms. */
  private inline fun cronometrar(f: () -> Unit): Long { val t = System.nanoTime(); f(); return (System.nanoTime() - t) / 1_000_000 }

  @Test fun sinUrlConfiguradaNoHaceNadaYDevuelveNull() {
    val c = ClienteServidor("")
    assertTrue(!c.configurado)
    assertTrue(cronometrar { assertNull(pedir(c)) } < 100)
  }

  @Test fun servidorCaidoDevuelveNullRapido() {
    // Un puerto libre en el que no escucha nadie: conexión rechazada.
    val puerto = ServerSocket(0).use { it.localPort }
    val ms = cronometrar { assertNull(pedir(ClienteServidor("http://127.0.0.1:$puerto"))) }
    assertTrue("tardó $ms ms", ms < 4_000)
  }

  @Test fun sinInternetHostDesconocidoDevuelveNull() {
    assertNull(pedir(ClienteServidor("https://brasa-servidor.invalid")))
  }

  @Test fun errorDelServidorDevuelveNull() {
    assertNull(pedir(ClienteServidor(servidorFalso(500, """{"error":{"codigo":"interno"}}"""))))
    assertNull(pedir(ClienteServidor(servidorFalso(429, """{"error":{"codigo":"demasiadas_peticiones"}}"""))))
  }

  @Test fun respuestaMalFormadaDevuelveNull() {
    assertNull(pedir(ClienteServidor(servidorFalso(200, "<html>no es json</html>"))))
  }

  @Test fun respuestaValidaDevuelveLaFrase() {
    assertEquals("Respira conmigo.", pedir(ClienteServidor(servidorFalso(200, """{"frase":"Respira conmigo.","origen":"ia"}"""))))
  }
}
