package org.airpnp.http

import com.sun.net.httpserver.HttpExchange

class Response(private val he: HttpExchange) {

  def respond(data: String, code: Int = 200, contentType: String = "text/plain", chunked: Boolean = false) {
    val bytes = data.getBytes("UTF8")
    doRespond(bytes, code, contentType + "; charset=utf-8", chunked)
  }

  def respondRaw(data: Array[Byte], code: Int = 200, contentType: String = "text/plain", chunked: Boolean = false) {
    doRespond(data, code, contentType, chunked)
  }

  private def doRespond(data: Array[Byte], code: Int, contentType: String, chunked: Boolean) {
    he.getResponseHeaders().add("Content-Type", contentType)
    he.sendResponseHeaders(code, if (chunked) 0 else data.length)
    val os = he.getResponseBody
    os.write(data)
    // From the API doc:
    // Exchanges are terminated when both the request InputStream and response OutputStream are closed. 
    // Closing the OutputStream, implicitly closes the InputStream (if it is not already closed).
    os.close
  }

}