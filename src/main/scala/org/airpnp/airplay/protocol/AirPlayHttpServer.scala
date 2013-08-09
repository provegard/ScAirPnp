package org.airpnp.airplay.protocol

import scala.collection.JavaConversions._
import java.net.InetSocketAddress
import org.airpnp.airplay.AirPlayDevice
import org.airpnp.http.RoutingHttpServer
import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import org.airpnp.Logging

class AirPlayHttpServer(address: InetSocketAddress, apDevice: AirPlayDevice) extends RoutingHttpServer(address) {
//  addFilter(new AirPlayHttpServer.InspectFilter())
  
  addRoute("/server-info", new ServerInfoRoute(apDevice))
  addRoute("/playback-info", new PlaybackInfoRoute(apDevice))
  addRoute("/slideshow-features", new SlideshowFeaturesRoute())
  addRoute("/play", new PlayRoute(apDevice))
  addRoute("/stop", new StopRoute(apDevice))
  addRoute("/scrub", new ScrubRoute(apDevice))
  addRoute("/rate", new RateRoute(apDevice))
  addRoute("/setProperty", new SetPropertyRoute(apDevice))
  addRoute("/photo", new PhotoRoute(apDevice))
  addRoute("/reverse", new ReverseRoute(apDevice))
}

//object AirPlayHttpServer {
//  class InspectFilter extends Filter with Logging {
//    def doFilter(exchange: HttpExchange,
//      chain: Filter.Chain) {
//
//      trace("Request from {}: {} {}", exchange.getRemoteAddress().toString,
//        exchange.getRequestMethod(), exchange.getRequestURI().toString)
//      trace("-- Request headers:")
//      for (h <- exchange.getRequestHeaders().entrySet()) {
//        trace("--  {}: {}", h.getKey, h.getValue.mkString(", "))
//      }
//
//      chain.doFilter(exchange)
//    }
//
//    def description() = "Inspect HTTP request headers"
//  }
//
//}