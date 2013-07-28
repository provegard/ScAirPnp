package org.airpnp.actor

import scala.actors.Actor
import scala.actors.Actor._
import scala.xml.Node
import org.airpnp.upnp.Device
import org.airpnp.Logging

case class Build(val udn: String, val location: String)
case class DeviceShouldBeIgnored(val udn: String, val reason: String)
case class DeviceReady(val device: Device)

class DeviceBuilder(private val download: String => Node) extends Actor with Logging {

  //TODO: Find a better way to reference this info in exceptionHandler
  private var currentUdn: String = null
  
  override def exceptionHandler = {
    case e: Exception => {
      sender ! DeviceShouldBeIgnored(currentUdn, "build error: " + e.getMessage)
    }
  }
  
  def act() = {
    loop {
      react {
        case b: Build => {
          currentUdn = b.udn
          debug("Building device from {}.", b.location)
          
          val node = download(b.location)
          val device = new Device(node, b.location)
          
          if (device.isMediaRenderer) {
            trace("Initializing services for device {}.", device.getFriendlyName)
            device.getServices.foreach(s => s.initialize(download(s.getSCPDURL)))
            sender ! DeviceReady(device)
          } else {
            debug("Ignoring device {} because it's not a media renderer.", device.getFriendlyName)
            sender ! DeviceShouldBeIgnored(device.getUdn, "not a media renderer")
          }
        }
        case Stop => {
          trace("Device builder was stopped.")
          sender ! Stopping
          exit
        }
      }
    }
  }
}