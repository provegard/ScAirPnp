package org.airpnp.upnp

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import org.airpnp.airplay.BaseAirPlayDevice
import org.airpnp.airplay.DurationAndPosition
import org.airpnp.Logging
import org.airpnp.upnp.UPNP.{ toDuration, parseDuration }
import scala.concurrent.Future
import java.io.InputStream

object AirPlayBridge {
  private val AVTRANSPORT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"
}

class AirPlayBridge(private val device: Device,
  private val sender: (String, SoapMessage) => Future[SoapMessage]) extends BaseAirPlayDevice(device.getFriendlyName, device.getUdn) with Logging {

  //TODO: Assume that required actions exist here, verify when we build the device!!!
  // Only Pause is optional!!

  //TODO: Logging! Wrap sender and trace command/reply

  private val instanceId = 0
  private val avTransport = device.getServices.find(s => s.getServiceType == AirPlayBridge.AVTRANSPORT_SERVICE_TYPE).get
  private val controlUrl = avTransport.getControlURL
  private def createMessage(a: Action, params: (String, Any)*) = a.createSoapMessage(("InstanceID", instanceId) +: params: _*)

  def getScrub() = {
    val a = avTransport.action("GetPositionInfo").get
    sender.apply(controlUrl, createMessage(a)).map {
      case reply => {
        val d = parseDuration(reply.getArgument("TrackDuration", "0:00:00"))
        val p = parseDuration(reply.getArgument("RelTime", "0:00:00"))
        new DurationAndPosition(d, p)
      }
    }
  }

  def isPlaying() = {
    val a = avTransport.action("GetTransportInfo").get
    sender.apply(controlUrl, createMessage(a)).map {
      case reply => reply.getArgument("CurrentTransportState", "") == "PLAYING"
    }
  }

  def play(location: String, position: Double) = {
    val a = avTransport.action("SetAVTransportURI").get
    val msg = createMessage(a, ("CurrentURI", location), ("CurrentURIMetaData", ""))
    sender.apply(controlUrl, msg).map {
      case _ => ()
    }
    //TODO: Save position, we cannot set it until the device knows the location
  }

  def setProperty(name: String, value: Any) = {
    //TODO: Debug Log
    future { () }
  }

  def setRate(rate: Double) = {
    //TODO: Debug log
    {
      if (rate >= 1) {
        val a = avTransport.action("Play").get
        val msg = createMessage(a, ("Speed", "1"))
        sender.apply(controlUrl, msg)
      } else {
        // Pause is optional
        avTransport.action("Pause") match {
          case Some(a) => {
            sender.apply(controlUrl, createMessage(a))
          }
          case None => future { () } //TODO: warn that Pause is missing
        }
      }
    }.map {
      case _ => ()
    }
  }

  def setScrub(position: Double) = {
    val hms = toDuration(position)
    val a = avTransport.action("Seek").get
    val msg = createMessage(a, ("Unit", "REL_TIME"), ("Target", hms))
    sender.apply(controlUrl, msg).map {
      case _ => ()
    }
  }

  def showPhoto(data: => InputStream, length: Int, transition: String) = {
    //TODO: SetAVTransportURI + Play
    //Requires us to publish the photo...
    future { () }
  }

  def stop() = {
    //TODO: Debug log
    //TODO: Handle SOAP error generally, we can get 718 here and should ignore that!
    val a = avTransport.action("Stop").get
    val msg = createMessage(a)
    sender.apply(controlUrl, msg).map {
      case _ => ()
    }
  }
}