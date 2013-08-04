package org.airpnp.http;

import scala.language.implicitConversions
import java.io.InputStream;

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import scala.collection.JavaConversions._

class Request(private val he: HttpExchange) {

  private val queryMap: Map[String, Seq[String]] = he.getRequestURI.getQuery match {
    case s: String if s != null => {
      s.split("&").seq.map(splitParam).groupBy(_._1).mapValues(_.map(_._2))
    }
    case _ => Map.empty
  }

  private def splitParam(param: String) = param.split("=") match {
    case x: Array[String] if x.length == 1 => ("", x(0)) // unnamed
    case x: Array[String] if x.length > 1 => (x(0), x(1))
  }

  def getInputStream() = he.getRequestBody

  def getHeader(name: String) = {
    val result = he.getRequestHeaders().get(name)
    if (result == null) List.empty else result.toList
  }
  def getArgument(name: String) = queryMap.getOrElse(name, Seq.empty)
  
  def getMethod() = he.getRequestMethod
}