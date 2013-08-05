package org.airpnp.actor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.concurrent.duration.Duration.Inf
import org.airpnp.Logging
import scala.actors.Actor
import org.airpnp.airplay.AirPlayService
import org.airpnp.Util
import org.airpnp.upnp.AirPlayBridge
import java.net.InetAddress
import org.airpnp.airplay.MDnsServiceHost
import org.airpnp.airplay.protocol.AirPlayHttpServer
import java.net.InetSocketAddress
import org.airpnp.airplay.AirPlayDevice
import org.airpnp.http.RoutingHttpServer
import scala.collection.mutable.HashMap
import scala.concurrent.Await
import org.airpnp.upnp.Device

class DevicePublisher(private val mdnsHost: MDnsServiceHost, private val addr: InetAddress) extends Actor with Logging {
  //  private var comm: DeviceCommunicator = null
  //  private var apService: AirPlayService = null

  private val published = new HashMap[String, DevicePublisher.PublishedStuff]()

  def act() {
    loop {
      react {
        case x: Publish => {
          val port = Util.findPort
          val publishedStuff = new DevicePublisher.PublishedStuff(x.device,
            new InetSocketAddress(addr, port))
          publishedStuff.startAll(mdnsHost)
          published += ((x.device.getUdn, publishedStuff))
        }

        case Stop => {
          published.values.foreach(_.stopAll(mdnsHost))
          debug("Device publisher was stopped.")
          sender ! Stopped
          exit
        }
      }
    }
  }
}

object DevicePublisher {
  private class PublishedStuff(val device: Device, val addr: InetSocketAddress) extends Logging {

    private val comm = new DeviceCommunicator(device)
    private val bridge = new AirPlayBridge(device, comm.createSoapSender())
    private val httpServer = new AirPlayHttpServer(addr, bridge)
    private val apService = new AirPlayService(bridge, addr.getPort())

    def startAll(mdnsHost: MDnsServiceHost) {
      debug("Starting HTTP server at {} and registering AirPlay service for device {}.",
        addr.toString, device.getFriendlyName)
      comm.start()
      httpServer.start()
      mdnsHost.register(apService.getService)
    }

    def stopAll(mdnsHost: MDnsServiceHost) {
      debug("Stopping HTTP server and unregistering AirPlay service for device {}.",
          device.getFriendlyName)
      mdnsHost.unregister(apService.getService)
      httpServer.stop(0)
      val f = bridge.isPlaying flatMap { playing: Boolean =>
        (if (playing) bridge.stop() else future { () }).map(_ => comm !? Stop)
      }
      Await.result(f, Inf)
    }
  }
}