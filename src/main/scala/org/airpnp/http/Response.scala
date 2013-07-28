package org.airpnp.http

import com.sun.net.httpserver.HttpExchange

class Response(private val he: HttpExchange) {

  def respond(code: Int, contentType: String, data: String): Unit = {
    val bytes = data.getBytes("UTF8")
    respond(code, contentType + "; charset=utf-8", bytes)
  }

  def respond(code: Int, contentType: String, data: Array[Byte]): Unit = {
    he.getResponseHeaders().add("Content-Type", contentType)
    he.sendResponseHeaders(code, data.length)
    val os = he.getResponseBody
    os.write(data)
    os.close
  }

  def respond(data: String): Unit = respond(200, "text/plain", data)
}