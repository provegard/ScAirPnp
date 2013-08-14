package org.airpnp.actor

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import org.airpnp.Logging
import scala.actors.Actor
import org.airpnp.upnp.Device
import org.airpnp.upnp.SoapMessage
import scala.util.Try
import org.airpnp.upnp.SoapClient
import scala.concurrent.Promise
import scala.util.Success
import scala.util.Failure

object DeviceCommunicator {
  private case class Message(val url: String, val msg: SoapMessage)
  private case class Reply(val reply: Try[SoapMessage])
}

class DeviceCommunicator(private val device: Device) extends BaseActor {
  device.soapSender = createSoapSender()
  private val client = new SoapClient()

  override def toString() = "Device communicator for " + device.getFriendlyName

  def act() {
    loop {
      //TODO: Use reactWithin, and on TIMEOUT do a liveness check
      react {
        case Touch => {
          //TODO: Update last seen timestamp
        }
        case CheckLiveness => {
          //TODO: Ping the device
        }
        case Stop =>
          device.soapSender = null
          debug("Communicator for device {} was stopped.", device.getFriendlyName)
          sender ! Stopped
          exit

        case m: DeviceCommunicator.Message =>
          //TODO: If the reply is not a timeout/socket error, update the timestamp
          val t = Try(client.sendMessage(m.url, m.msg))
          sender ! DeviceCommunicator.Reply(t)
      }
    }
  }

  private def createSoapSender(): (String, SoapMessage) => scala.concurrent.Future[SoapMessage] = {
    (url, msg) =>
      {
        val p = Promise[SoapMessage]()
        this !! (DeviceCommunicator.Message(url, msg), {
          case r: DeviceCommunicator.Reply => p.complete(r.reply)
        })

        p.future
      }
  }
}