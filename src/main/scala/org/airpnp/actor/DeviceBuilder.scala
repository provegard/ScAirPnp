package org.airpnp.actor

import scala.actors.Actor
import scala.actors.Actor._
import scala.xml.Node
import org.airpnp.upnp.Device
import org.airpnp.Logging
import scala.util.Try
import scala.util.Failure
import scala.util.Success

class DeviceBuilder(private val download: String => Node) extends BaseActor {

  override def toString() = "Device builder"

  def act() = {
    loop {
      react {
        case b: Build =>
          try {
            debug("Building new device with UDN {} from {}.", b.udn, b.location)

            val node = download(b.location)
            val device = new Device(node, b.location)

            if (device.isMediaRenderer || b.force) {
              device.getServices.foreach(s => s.initialize(download(s.getSCPDURL)))
              sender ! DeviceReady(device)
            } else {
              sender ! DeviceShouldBeIgnored(Some(device), b.udn, "not a media renderer")
            }
          } catch {
            case t: Throwable =>
              error("Failed to build device from " + b.location, t)
              sender ! DeviceShouldBeIgnored(None, b.udn, "build error: " + t.getMessage)
          }

        case Stop =>
          debug("Device builder was stopped.")
          sender ! Stopped
          exit
      }
    }
  }
}