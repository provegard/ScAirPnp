
package org.airpnp

import java.net.DatagramPacket
import scala.actors.Actor
import org.airpnp.actor.Coordinator
import org.airpnp.actor.CoordinatorOptions
import org.airpnp.actor.DeviceBuilder
import org.airpnp.actor.DeviceDiscovery
import org.airpnp.actor.DeviceFound
import org.airpnp.actor.DevicePublisher
import org.airpnp.actor.Stop
import org.airpnp.airplay.DefaultMDnsServiceHost
import org.airpnp.airplay.MDnsServiceHost
import org.airpnp.dlna.AirPnpFolder
import org.airpnp.plumbing.InterceptingDatagramSocketImplFactory
import org.airpnp.upnp.UPnPMessage
import net.pms.dlna.DLNAResource
import net.pms.external.AdditionalFolderAtRoot
import net.pms.external.ExternalListener
import net.pms.PMS
import org.airpnp.actor.ADLNAPublisher

class AirPnp extends ExternalListener with AdditionalFolderAtRoot with Logging with TestMode {

  private var coordinator: Coordinator = null
  private var mdnsHost: MDnsServiceHost = null
  private var rootFolder: AirPnpFolder = null

  info("AirPnp plugin starting!")
  if (!Util.hasJDKHttpServer) {
    error("AirPnp needs a JDK rather than a JRE for HTTP server support.")
  } else {
    rootFolder = new AirPnpFolder({PMS.get.getServer.getURL})
    maybeAddTestContent(rootFolder)
    
    val addr = Networking.getInetAddress
    mdnsHost = new DefaultMDnsServiceHost()
    mdnsHost.start(addr)
    val db = new DeviceBuilder(Networking.createDownloader())
    val dd = new DeviceDiscovery
    val dl = new ADLNAPublisher(rootFolder)
    val dp = new DevicePublisher(mdnsHost, addr, dl)

    val options = new CoordinatorOptions()
    options.deviceBuilder = db
    options.deviceDiscovery = dd
    options.devicePublisher = dp
    options.dlnaPublisher = dl
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

  def shutdown() {
    info("AirPnp plugin shutting down!")
    mdnsHost.stop()
    coordinator !? Stop
  }
  
  def getChild(): DLNAResource = rootFolder
}