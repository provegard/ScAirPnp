package org.airpnp.actor

import scala.actors.Actor
import scala.actors.Actor._
import scala.xml.Node
import org.airpnp.upnp.Device
import org.airpnp.Logging

class DeviceBuilder(private val download: String => Node) extends Actor with Logging {

  //TODO: Find a better way to reference this info in exceptionHandler
  private var currentUdn: String = null

  override def exceptionHandler = {
    case e: Exception => {
      sender ! DeviceShouldBeIgnored(None, currentUdn, "build error: " + e.getMessage)
    }
  }

  def act() = {
    loop {
      react {
        case b: Build =>
          currentUdn = b.udn
          debug("Building new device with UDN {} from {}.", b.udn, b.location)

          val node = download(b.location)
          val device = new Device(node, b.location)

          if (device.isMediaRenderer) {
            device.getServices.foreach(s => s.initialize(download(s.getSCPDURL)))
            sender ! DeviceReady(device)
          } else {
            sender ! DeviceShouldBeIgnored(Some(device), b.udn, "not a media renderer")
          }

        case Stop =>
          debug("Device builder was stopped.")
          sender ! Stopped
          exit
      }
    }
  }
}