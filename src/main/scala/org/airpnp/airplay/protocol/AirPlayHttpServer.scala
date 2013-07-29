package org.airpnp.airplay.protocol

import java.net.InetSocketAddress
import org.airpnp.airplay.AirPlayDevice
import org.airpnp.http.RoutingHttpServer

class AirPlayHttpServer(private val address: InetSocketAddress, private val apDevice: AirPlayDevice) extends RoutingHttpServer(address) {
  addRoute("/server-info", new ServerInfoRoute(apDevice))
  addRoute("/playback-info", new PlaybackInfoRoute(apDevice))
  addRoute("/slideshow-features", new SlideshowFeaturesRoute())
  addRoute("/play", new PlayRoute(apDevice))
  addRoute("/stop", new StopRoute(apDevice))
  addRoute("/scrub", new ScrubRoute(apDevice))
  addRoute("/rate", new RateRoute(apDevice))
  addRoute("/setProperty", new SetPropertyRoute(apDevice))
  addRoute("/photo", new PhotoRoute(apDevice))
}