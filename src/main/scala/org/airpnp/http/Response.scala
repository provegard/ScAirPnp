package org.airpnp.http

import com.sun.net.httpserver.HttpExchange
import java.nio.charset.Charset

class Response(he: HttpExchange) {

  private var closeIt = true

  def dontClose() {
    closeIt = false
  }

  def addHeader(name: String, value: String) {
    he.getResponseHeaders().add(name, value)
  }

  def respond(response: Response.ResponseData) {
    val hasData = response.hasData
    if (hasData) {
      he.getResponseHeaders().add("Content-Type", response.getContentType)
    }
    // -1 means no content
    val cl = if (hasData) (if (response.isChunked) 0 else response.data.length) else -1
    he.sendResponseHeaders(response.code, cl)
    val os = he.getResponseBody
    if (hasData) {
      os.write(response.data)
    }
    // From the API doc:
    // Exchanges are terminated when both the request InputStream and response OutputStream are closed. 
    // Closing the OutputStream, implicitly closes the InputStream (if it is not already closed).
    if (closeIt) {
      os.close
    }
  }

}

object Response {
  class ResponseData private[http] (private[http] var code: Int,
    private[http] var data: Array[Byte]) {
    private var contentType = "text/plain"
    private[http] var isChunked = false
    private[http] var isUtf8 = false

    private[http] def hasData = data != null && data.length > 0

    def andContentType(ct: String) = {
      contentType = ct
      this
    }

    def andIsChunked() = {
      isChunked = true
      this
    }

    def andStatusCode(status: Int) = {
      code = status
      this
    }

    def andUtf8Text(text: String) = {
      data = text.getBytes("UTF-8")
      isUtf8 = true
      this
    }

    def andText(text: String) = {
      data = text.getBytes("US-ASCII")
      isUtf8 = false
      this
    }

    private[http] def getContentType = contentType + (if (isUtf8) "; charset=utf-8" else "")
  }

  def withStatus(status: Int): ResponseData = new ResponseData(status, null)
  def withText(text: String): ResponseData = new ResponseData(200, null).andText(text)
  def withUtf8Text(text: String): ResponseData = new ResponseData(200, null).andUtf8Text(text)
}