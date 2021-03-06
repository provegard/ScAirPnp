package org.airpnp;

import scala.language.implicitConversions
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.util.ArrayList
import java.util.Arrays
import java.util.Iterator
import java.util.List
import java.util.Map
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global

object Util {

  class FutureCombinator[T] private[airpnp] (f: Future[T]) {
    def followWith[U](other: => Future[U]): Future[U] = {
      val p = Promise[U]()
      f.onComplete {
        case Success(result) =>
          other.onComplete {
            case Success(x) => p.success(x)
            case Failure(t) => p.failure(t)
          }
        case Failure(t) => p.failure(t)
      }
      p.future
    }
  }

  implicit def combinefuture[T](f: Future[T]): FutureCombinator[T] = new FutureCombinator(f)

  /**
   * Split a USN into a UDN and a device or service type.
   * <p>
   * USN is short for Unique Service Name, and UDN is short for Unique Device
   * Name. If the USN only contains a UDN, the type is empty.
   *
   * @param usn
   *            the USN to split
   * @return a tuple of (UDN, device/service type)
   */
  def splitUsn(usn: String): (String, String) = {
    val parts = usn.split("::")
    if (parts.length == 2) (parts(0), parts(1)) else (parts(0), "")
  }

  def areServiceTypesCompatible(required: String, actual: String): Boolean = {
    if (required == actual)
      return true
    var rparts = splitServiceType(required)
    var aparts = splitServiceType(actual)

    if (rparts._1 != aparts._1)
      return false
    if (rparts._2 == aparts._2)
      return true
    try {
      return Integer.parseInt(aparts._2) > Integer.parseInt(rparts._2)
    } catch {
      // Not numbers, and different
      case e: NumberFormatException => return false
    }
  }

  /**
   * Parse the 'max-age' directive from the 'CACHE-CONTROL' header.
   *
   * @param headers
   *            dictionary of HTTP headers
   * @return the parsed value as an integer, or -1 if the 'max-age' directive
   *         or the 'CACHE-CONTROL' header couldn't be found, or if the header
   *         is invalid in any way.
   */
  def getMaxAge(headers: Map[String, String]): Int = {
    val cache_control = headers.get("CACHE-CONTROL")
    if (cache_control != null) {
      val directives = cache_control.split("\\s*,\\s*")
      for (directive <- directives) {
        val parts = directive.split("\\s*=\\s*")
        if (parts.length == 2 && "max-age" == parts(0)
          && parts(1).matches("^\\d+$")) {
          return Integer.parseInt(parts(1))
        }
      }
    }
    -1
  }

  private def splitServiceType(st: String): (String, String) = {
    val idx = st.lastIndexOf(':')
    if (idx < 0) {
      return (st, "")
    }
    (st.substring(0, idx), st.substring(idx + 1))
  }

  def createDeviceId(data: String): String = {
    val h = data.hashCode() & 0x00000000ffffffffL //TODO: spread the bits!
    val hx = "%012X".format(h).substring(0, 12)
    (for (i <- 0 until 12 by 2) yield hx.substring(i, i + 2)).mkString(":")
  }

  def findPort() = {
    var serverSocket: ServerSocket = null;
    try {
      serverSocket = new ServerSocket(0)
      serverSocket.getLocalPort()
    } finally {
      serverSocket.close
    }
  }

  def readAllBytes(is: InputStream): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    val buf = new Array[Byte](8192)
    var len = is.read(buf)
    while (len != -1) {
      bos.write(buf, 0, len)
      len = is.read(buf)
    }
    bos.toByteArray()
  }

  def hasJDKHttpServer() = {
    try {
      Class.forName("com.sun.net.httpserver.HttpServer")
      true
    } catch {
      case _: ClassNotFoundException => false
    }
  }
}
