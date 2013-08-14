package org.airpnp.upnp

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.URL

import scala.collection.mutable.HashMap
import scala.collection.mutable.MutableList

/**
 * Simple HTTP/SOAP client. We could use HttpURLConnection, except it doesn't
 * support M-POST...
 */
class SoapClient {

  def sendMessage(url: String, message: SoapMessage) = send(new URL(url), message)

  private def send(url: URL, message: SoapMessage, useMPost: Boolean = false): SoapMessage = {
    val socket = new Socket(InetAddress.getByName(url.getHost), url.getPort)
    socket.setSoTimeout(2000) //TODO: Investigate suitable timeout
    try {
      val data = message.toString.getBytes("UTF-8")
      val headers = new MutableList[(String, String)]()
      if (useMPost) {
        headers += (("MAN", "\"http://schemas.xmlsoap.org/soap/envelope/\"; ns=01"))
        headers += (("01-SOAPACTION", message.getSoapAction))
      } else {
        headers += (("SOAPACTION", message.getSoapAction))
      }

      writeRequest(socket.getOutputStream, url, if (useMPost) "M-POST" else "POST", headers, data)
      val is = socket.getInputStream
      val responseHeaders = new HashMap[String, String]()
      val statusCode = readResponseHeaders(is, responseHeaders)

      statusCode match {
        case 200 => SoapMessage.parse(getData(responseHeaders, is))
        case 405 if !useMPost => send(url, message, true)
        case 500 => throw SoapError.parse(getData(responseHeaders, is))
        case _ => throw new IOException("Server responded with " + statusCode)
      }

    } finally {
      socket.close
    }
  }

  private def getData(responseHeaders: HashMap[String, String], is: InputStream): InputStream = {
    responseHeaders.get("CONTENT-LENGTH") match {
      case Some(cl) => {
        val len = Integer.parseInt(cl)
        new SoapClient.FixedLengthInputStream(is, len)
      }
      case _ => responseHeaders.get("TRANSFER-ENCODING") match {
        case Some(enc) if enc.toLowerCase == "chunked" => {
          new SoapClient.ChunkedInputStream(is)
        }
        case _ => is
      }
    }
  }

  private def writeRequest(os: OutputStream, url: URL, method: String, headers: Seq[(String, String)],
    data: Array[Byte]) {
    val sb = new StringBuilder()
    val allHeaders = Seq(("Host", url.getHost + ":" + url.getPort),
      ("Content-Length", data.length.toString),
      ("Content-Type", "text/xml; charset=\"utf-8\""),
      ("User-Agent", "OS/1.0 UPnP/1.0 AirPnp/0.1")) ++ headers
    sb.append("%s %s HTTP/1.1\r\n".format(method, url.getPath))
    allHeaders.foreach(pair => {
      sb.append("%s: %s\r\n".format(pair._1, pair._2))
    })
    sb.append("\r\n")
    val text = sb.toString
    os.write(sb.toString.getBytes("US-ASCII"))
    os.write(data)
    os.flush()
  }

  private def readResponseHeaders(is: InputStream, headers: HashMap[String, String]): Int = {
    val firstLine = SoapClient.readLine(is)
    val first = firstLine.split(" ")
    if (first.length < 3) {
      throw new IOException("Invalid HTTP status line: " + firstLine)
    }
    val statusCode = first(1).toInt
    while (true) {
      SoapClient.readLine(is) match {
        case x if x == "" => return statusCode
        case y => {
          val parts = y.split("\\s*:\\s*")
          parts.length match {
            case 1 => headers += ((parts(0).toUpperCase, ""))
            case 2 => headers += ((parts(0).toUpperCase, parts(1)))
            case _ => // ignore
          }
        }
      }
    }
    0
  }
}

object SoapClient {
  private def readLine(is: InputStream): String = {
    val sb = new StringBuilder()
    while (true) {
      is.read() match {
        case x if x == -1 || x == 10 => return sb.toString.trim
        case y => sb.append(y.asInstanceOf[Char])
      }
    }
    null
  }

  private class FixedLengthInputStream(private val orig: InputStream, private val len: Int) extends InputStream {
    private var left = len
    def read(): Int = {
      if (left > 0) {
        val ret = orig.read()
        left -= 1
        ret
      } else {
        -1
      }
    }
  }

  private class ChunkedInputStream(private val orig: InputStream) extends InputStream {
    private var left = 0
    private var eof = false

    def read(): Int = {
      if (left > 0) {
        val ret = orig.read()
        left -= 1
        ret
      } else if (eof) {
        -1
      } else {
        left = readChunkSize
        eof = left == 0
        if (eof) {
          consumeTrailer
        }
        read
      }
    }

    private def consumeTrailer() {
      while (true) {
        readLine(orig) match {
          case "" => return
          case _ =>
        }
      }
    }

    private def readChunkSize(): Int = {
      val line = readLine(orig)
      try {
        Integer.parseInt(line.split(";")(0), 16)
      } catch {
        case e: Exception => throw new IOException("Not a valid chunk size line: " + line)
      }
    }
  }
}