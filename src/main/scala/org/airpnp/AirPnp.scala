
package org.airpnp

import net.pms.external.ExternalListener
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import net.pms.PMS
import scala.collection.mutable.MutableList
import scala.actors.Actor
import org.airpnp.actor.DeviceBuilder
import org.airpnp.actor.Coordinator
import org.airpnp.actor.Stop
import org.airpnp.plumbing.InterceptingDatagramSocketImplFactory
import org.airpnp.upnp.UPnPMessage
import org.airpnp.actor.DeviceFound
import org.airpnp.actor.DeviceDiscovery
import java.net.DatagramPacket
import org.airpnp.actor.DevicePublisher
import org.airpnp.actor.CoordinatorOptions
import org.airpnp.airplay.MDnsServiceHost

class AirPnp extends ExternalListener with Logging {

  private var coordinator: Coordinator = null
  private var mdnsHost: MDnsServiceHost = null

  info("AirPnp plugin starting!")
  if (!Util.hasJDKHttpServer) {
    error("AirPnp needs a JDK rather than a JRE for HTTP server support.")
  } else {
    val addr = Networking.getInetAddress
    mdnsHost = new MDnsServiceHost()
    mdnsHost.start(addr)
    val db = new DeviceBuilder(Networking.createDownloader())
    val dd = new DeviceDiscovery
    val dp = new DevicePublisher(mdnsHost, addr)

    val options = new CoordinatorOptions()
    options.deviceBuilder = db
    options.deviceDiscovery = dd
    options.devicePublisher = dp
    options.address = addr

    coordinator = new Coordinator(options)
    coordinator.start()

    InterceptingDatagramSocketImplFactory.install(packetInterceptor(coordinator))
  }

  //TODO: Move the function somewhere else...
  private def packetInterceptor(target: Actor)(packet: DatagramPacket) = {
    val msg = new UPnPMessage(packet)
    if (msg.isBuildable) {
      Actor.actor { target ! DeviceFound(msg.getUdn.get, msg.getLocation.get) }
    }
  }

  def config(): javax.swing.JComponent = null

  def name(): String = "AirPnp"

  def shutdown(): Unit = {
    info("AirPnp plugin shutting down!")
    mdnsHost.stop()
    coordinator !? Stop
  }
}