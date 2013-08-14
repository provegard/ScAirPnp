
package org.airpnp

import scala.concurrent.ExecutionContext.Implicits.global
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
import org.airpnp.actor.Scheduling
import org.airpnp.actor.TriggerPMSFolderDiscovery
import org.airpnp.actor.TriggerPMSFolderDiscovery
import java.net.URL
import scala.concurrent.Promise
import scala.util.Try
import org.airpnp.actor.MaybePublishTestContent
import org.airpnp.actor.TestMode
import scala.util.Success
import org.airpnp.actor.GetPublishedDevices
import org.airpnp.actor.GetPublishedDevicesReply
import org.airpnp.ui.AirPnpPanel

class AirPnp extends ExternalListener with AdditionalFolderAtRoot with Logging with TestMode {

  private var coordinator: Coordinator = null
  private var mdnsHost: MDnsServiceHost = null
  private var rootFolder: AirPnpFolder = null

  if (!Util.hasJDKHttpServer) {
    error("AirPnp needs a JDK rather than a JRE for HTTP server support.")
  } else {
    info("AirPnp plugin is starting up!")
    // Create our root folder, which also happens to by a DLNA publisher.
    // The URL arg is by-name so it is "lazily" evaluated, which is necessary
    // since PMS's HTTPServer hasn't started yet, so the URL is invalid.
    rootFolder = new AirPnpFolder({ PMS.get.getServer.getURL })

    // Find out which network address we should listen on.
    val addr = Networking.getInetAddress

    // Start the host for mDNS services.
    mdnsHost = new DefaultMDnsServiceHost()
    mdnsHost.start(addr)

    // Assemble actors and ultimately the Coordinator actor. Don't start
    // things yet.
    val db = new DeviceBuilder(Networking.createDownloader())
    val dd = new DeviceDiscovery
    val dl = new ADLNAPublisher(rootFolder) // actor wrapper
    val dp = new DevicePublisher(mdnsHost, addr, dl)

    val options = new CoordinatorOptions()
    options.deviceBuilder = db
    options.deviceDiscovery = dd
    options.devicePublisher = dp
    options.dlnaPublisher = dl
    options.address = addr

    coordinator = new Coordinator(options)

    // Install a socket factory that creates datagram sockets that allow
    // us to intercept UPnP traffic.
    InterceptingDatagramSocketImplFactory.install(packetInterceptor(coordinator))

    waitForPmsHttpServer(250).future.map {
      case _ =>
        info("AirPnp detected that the PMS HTTP server has started, startup continues.")
        coordinator.start()

        val p = Promise[Unit]()
        coordinator ! TriggerPMSFolderDiscovery(PMS.get.usn,
          new URL(new URL(PMS.get.getServer.getURL), "/description/fetch").toString, p)
          
        p.future.andThen {
          case Success(_) => coordinator ! MaybePublishTestContent
        }
    }
  }

  private def waitForPmsHttpServer(interval: Long): Promise[Unit] = {
    val p = Promise[Unit]()
    trace("Waiting for PMS's HTTP server to be started with interval {} ms...", interval)
    var s: Actor = null
    s = Scheduling.scheduler(interval, interval) {
      val server = PMS.get.getServer
      if (server.getHost() != null) {
        trace("PMS's HTTP server has started!")
        s ! Stop
        p.success(())
      } else {
        trace("PMS's HTTP server has not started yet.")
      }
    }
    p
  }

  //TODO: Move the function somewhere else...
  private def packetInterceptor(target: Actor)(packet: DatagramPacket) = {
    val msg = new UPnPMessage(packet)
    if (msg.isBuildable) {
      Actor.actor { target ! DeviceFound(msg.getUdn.get, msg.getLocation.get) }
    }
  }

  def config() = coordinator !? GetPublishedDevices() match {
    //TODO: pass downloader also
    case GetPublishedDevicesReply(devices) => new AirPnpPanel(devices)
    case _ => null
  }

  def name(): String = "AirPnp"

  def shutdown() {
    info("AirPnp plugin shutting down!")
    if (mdnsHost != null) {
      mdnsHost.stop()
    }
    if (coordinator != null) {
      coordinator !? Stop
    }
  }

  def getChild(): DLNAResource = {
    trace("AirPnp's additional folder at root was requested.")
    rootFolder
  }
}