package org.airpnp.actor

import scala.actors.Actor
import scala.actors.TIMEOUT
import org.airpnp.upnp.Device
import org.airpnp.upnp.SoapMessage

case object Stop // for stopping actors
case object Stopped // reply message when an actor is stopped

case class DoLivenessCheck(val devices: Seq[Device])
case class DeviceIsGone(val udn: String)
case class Build(val udn: String, val location: String)
case class DeviceShouldBeIgnored(val udn: String, val reason: String)
case class DeviceReady(val device: Device)
case object DoDiscovery
case class DeviceFound(val udn: String, val location: String)
case class Publish(val device: Device)
case object Touch
case object CheckLiveness

object Scheduling {
  // http://stackoverflow.com/questions/1224342/sleeping-actors
  def scheduler(time: Long)(f: => Unit) = {
    def fixedRateLoop {
      Actor.reactWithin(time) {
        case TIMEOUT =>
          f; fixedRateLoop
        case Stop =>
      }
    }
    Actor.actor(fixedRateLoop)
  }
}