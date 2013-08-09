package org.airpnp.http

import scala.collection.JavaConversions._
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import org.airpnp.Logging
import com.sun.net.httpserver.Filter
import scala.collection.mutable.MutableList

object RoutingHttpServer {
  private class Root extends HttpHandler with Logging {
    def handle(t: HttpExchange) = {
      debug("{} request for unknown path {} from {}.", t.getRequestMethod,
        t.getRequestURI.toString, t.getRemoteAddress.toString)
      for (h <- t.getRequestHeaders.entrySet()) {
        debug("Request header: {}: {}", h.getKey, h.getValue.mkString(", "))
      }
      val data = "Not found"
      t.sendResponseHeaders(404, data.length)
      val os = t.getResponseBody
      os.write(data.getBytes)
      os.close
    }
  }

  private class Router(private val handler: RouteHandler) extends HttpHandler with Logging {

    def handle(t: HttpExchange) = {
      trace("Request for path {} from {}.", t.getRequestURI.toString, t.getRemoteAddress.toString)

      t.getRequestMethod.toLowerCase match {
        case "get" => handler.handleGET(new Request(t), new Response(t))
        case "post" => handler.handlePOST(new Request(t), new Response(t))
        case "put" => handler.handlePUT(new Request(t), new Response(t))
        case _ => handler.handleUnknown(new Request(t), new Response(t))
      }
    }
  }
}

class RoutingHttpServer(private val address: InetSocketAddress) extends Logging {

  private val server = HttpServer.create(address, 20)
  server.createContext("/", new RoutingHttpServer.Root)
  server.setExecutor(null) // creates a default executor
  val filters = new MutableList[Filter]

  def addFilter(filter: Filter) {
    filters += filter
  }
  
  def addRoute(url: String, handler: RouteHandler) {
    val ctx = server.createContext(url, new RoutingHttpServer.Router(handler))
    filters.foreach(ctx.getFilters().add(_))
  }

  def start() {
    debug("Starting HTTP server on {}.", address.toString)
    server.start
  }
  def stop(wait: Int) {
    debug("Stopping HTTP server on {}, waiting {} second(s).", address.toString, wait.toString)
    server.stop(wait)
  }

}
