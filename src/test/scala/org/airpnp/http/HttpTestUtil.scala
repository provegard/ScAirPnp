package org.airpnp.http

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import org.airpnp.Util
import java.util.regex.Pattern
import java.nio.charset.Charset

object HttpTestUtil {
  private val CharsetPattern = Pattern.compile("charset=(.*)$")

  def openUrlConnection(path: String, port: Int) = {
    val u = new URL("http://localhost:" + port + path)
    u.openConnection()
  }

  def postDataToUrl(path: String, port: Int, contentType: String,
    data: Array[Byte]) = sendDataToUrl("POST", path, port, contentType, data, null)

  def readTextAndClose(conn: URLConnection): String = {
    val ct = conn.getHeaderField("Content-Type")
    readTextAndClose(conn.getInputStream(), ct)
  }
  
  def readTextAndClose(is: InputStream, contentType: String): String = {
    var charset = "us-ascii"
    if (contentType != null) {
      val matcher = CharsetPattern.matcher(contentType)
      if (matcher.find()) {
        charset = matcher.group(1)
      }
    }
    try {
      val s = new java.util.Scanner(is, charset).useDelimiter("\\A")
      if (s.hasNext()) s.next() else ""
    } finally {
      is.close()
    }
  }
  
  def readAllBytesAndClose(is: InputStream) = {
    try {
      Util.readAllBytes(is)
    } finally {
      is.close
    }
  }

  def putDataToUrl(path: String, port: Int, contentType: String,
    data: Array[Byte]) = sendDataToUrl("PUT", path, port, contentType, data, null)

  private def sendDataToUrl(method: String, path: String, port: Int, contentType: String,
    data: Array[Byte], headers: Map[String, String]) = {
    val connection = new URL("http://localhost:" + port + path)
      .openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod(method)
    connection.setDoOutput(true)
    if (headers != null) {
      headers.foreach(e => connection.setRequestProperty(e._1, e._2))
    }
    if (contentType != null) {
      connection.setRequestProperty("Content-Type", contentType)
    }
    var output: OutputStream = null
    try {
      output = connection.getOutputStream
      output.write(data)
      output.flush
    } finally {
      if (output != null) {
        output.close
      }
    }
    connection
  }

  def putDataToUrl(path: String, port: Int, contentType: String,
    data: Array[Byte], headers: Map[String, String]) = sendDataToUrl("PUT", path, port, contentType, data, headers)
}
