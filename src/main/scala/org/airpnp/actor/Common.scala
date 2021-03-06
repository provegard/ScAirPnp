package org.airpnp.actor

import scala.actors.Actor
import scala.actors.TIMEOUT
import org.airpnp.upnp.Device
import org.airpnp.upnp.SoapMessage
import scala.util.Try
import scala.concurrent.Promise

case object Stop // for stopping actors
case object Stopped // reply message when an actor is stopped

case class DoLivenessCheck(val devices: Seq[Device])
case class DeviceIsGone(val udn: String)
case class Build(val udn: String, val location: String, force: Boolean = false)
case class DeviceShouldBeIgnored(val device: Option[Device], val udn: String, val reason: String)
case class DeviceReady(val device: Device)
case object DoDiscovery
case class DeviceFound(val udn: String, val location: String)
case class Publish(val device: Device)
case object Touch
case object CheckLiveness
case class TriggerPMSFolderDiscovery(val udn: String, val location: String, val promise: Promise[Unit])
case object MaybePublishTestContent
case class GetPublishedDevices
case class GetPublishedDevicesReply(devices: Seq[Device])

object Scheduling {
  // http://stackoverflow.com/questions/1224342/sleeping-actors
  //TODO: Move into package object
  //TODO: Reply with Stopped!
  def scheduler(initialDelay: Long, time: Long)(f: => Unit) = {
    def fixedRateLoop {
      Actor.reactWithin(time) {
        case TIMEOUT =>
          f; fixedRateLoop
        case Stop =>
      }
    }
    def initialLoop {
      Actor.reactWithin(initialDelay) {
        case TIMEOUT =>
          f; fixedRateLoop
        case Stop =>
      }
    }
    Actor.actor(initialLoop)
  }
}