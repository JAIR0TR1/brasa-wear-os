package com.brasa.app

import com.brasa.app.data.ServerClient
import com.brasa.app.data.ActivityLevel
import com.brasa.app.data.Situation
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
class ServerOfflineTest {
  private var server: HttpServer? = null

  @After fun stopFakeServer() { server?.stop(0) }

  /** Servidor HTTP local que responde siempre [estado] con [cuerpo] tras [esperaMs]. */
  private fun fakeServer(state: Int, body: String, delayMs: Long = 0): String {
    val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    s.createContext("/") { ex ->
      Thread.sleep(delayMs)
      val b = body.toByteArray()
      ex.sendResponseHeaders(state, b.size.toLong())
      ex.responseBody.use { it.write(b) }
    }
    s.start()
    server = s
    return "http://127.0.0.1:${s.address.port}"
  }

  private fun requestPermissions(c: ServerClient) = runBlocking {
    c.message("3f6c2a1e-9b4d-4c7a-8e21-5d0b9a7f1c33", "es", Situation.TENSION, ActivityLevel.MEDIUM, "inquieta", "llama")
  }

  /** Tiempo de una llamada en ms. */
  private inline fun measureTimeMs(f: () -> Unit): Long { val t = System.nanoTime(); f(); return (System.nanoTime() - t) / 1_000_000 }

  @Test fun noUrlConfiguredDoesNothingAndReturnsNull() {
    val c = ServerClient("")
    assertTrue(!c.configured)
    assertTrue(measureTimeMs { assertNull(requestPermissions(c)) } < 100)
  }

  @Test fun serverDownReturnsNullQuickly() {
    // Un puerto libre en el que no escucha nadie: conexión rechazada.
    val port = ServerSocket(0).use { it.localPort }
    val ms = measureTimeMs { assertNull(requestPermissions(ServerClient("http://127.0.0.1:$port"))) }
    assertTrue("tardó $ms ms", ms < 4_000)
  }

  @Test fun noInternetUnknownHostReturnsNull() {
    assertNull(requestPermissions(ServerClient("https://brasa-servidor.invalid")))
  }

  @Test fun serverErrorReturnsNull() {
    assertNull(requestPermissions(ServerClient(fakeServer(500, """{"error":{"codigo":"interno"}}"""))))
    assertNull(requestPermissions(ServerClient(fakeServer(429, """{"error":{"codigo":"demasiadas_peticiones"}}"""))))
  }

  @Test fun malformedResponseReturnsNull() {
    assertNull(requestPermissions(ServerClient(fakeServer(200, "<html>no es json</html>"))))
  }

  @Test fun validResponseReturnsThePhrase() {
    assertEquals("Respira conmigo.", requestPermissions(ServerClient(fakeServer(200, """{"frase":"Respira conmigo.","origen":"ia"}"""))))
  }
}
