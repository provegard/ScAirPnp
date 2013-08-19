package org.airpnp.upnp

import scala.concurrent.future
import scala.concurrent.ExecutionContext.Implicits.global
import org.airpnp.airplay.BaseAirPlayDevice
import org.airpnp.airplay.DurationAndPosition
import org.airpnp.Logging
import org.airpnp.upnp.UPNP.{ toDuration, parseDuration }
import scala.concurrent.Future
import java.io.InputStream
import org.airpnp.dlna.DLNAPublisher
import scala.util.Success
import scala.util.Failure
import java.io.IOException
import scala.concurrent.Promise

object AirPlayBridge {
  private val AVTRANSPORT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"
}

class AirPlayBridge(device: Device,
  dlnaPublisher: DLNAPublisher) extends BaseAirPlayDevice(device.getFriendlyName, device.getUdn) with Logging {

  //TODO: Assume that required actions exist here, verify when we build the device!!!
  // Only Pause is optional!!

  private var playPosPct: Option[Double] = None
  private var traceLogId = 0
  private val instanceId = 0
  private val avTransport = device.getServices.find(s => s.getServiceType == AirPlayBridge.AVTRANSPORT_SERVICE_TYPE).get
  private val controlUrl = avTransport.getControlURL
  private def createMessage(a: Action, params: (String, Any)*) = a.createSoapMessage(("InstanceID", instanceId) +: params: _*)

  private val sender: Device.SoapSender = (url, msg) => {
    traceLogId += 1
    val id = traceLogId
    trace("[{}] Sending SOAP message to {} @ {}: {}", id, device.getFriendlyName, url, msg.toFunctionLikeString)
    device.soapSender(url, msg).andThen {
      case Success(reply) =>
        trace("[{}] Got SOAP reply from {}: {}", id, device.getFriendlyName, reply.toFunctionLikeString)
        reply
      case Failure(t) =>
        trace("[{}] Got error of type {} from {} with message: {}", id, t.getClass.getSimpleName,
          device.getFriendlyName, t.getMessage)
        t
    }
  }

  def getScrub() = {
    val p = Promise[DurationAndPosition]()
    val a = avTransport.action("GetPositionInfo").get
    sender.apply(controlUrl, createMessage(a)).map({
      reply =>
        {
          val d = parseDuration(reply.getArgument("TrackDuration", "0:00:00"))
          val p = parseDuration(reply.getArgument("RelTime", "0:00:00"))
          new DurationAndPosition(d, p)
        }
    }).onComplete {
      case Success(dp) => playPosPct match {
        case Some(pos) if dp.duration > 0 && pos * dp.duration > dp.position =>
          setScrub(pos * dp.duration).onComplete {
            case Success(_) =>
              // After seeking, the position before seeking isn't really correct, but
              // once the device is done seeking, a subsequent getScrub will return
              // the correct position.
              p.success(dp)
            case Failure(t) => p.failure(t)
          }
        case _ => p.success(dp)
      }
      case Failure(t) => p.failure(t)
    }
    p.future
  }

  def isPlaying() = {
    val a = avTransport.action("GetTransportInfo").get
    sender.apply(controlUrl, createMessage(a)).map {
      case reply => reply.getArgument("CurrentTransportState", "") == "PLAYING"
    }
  }

  def play(location: String, position: Double) = {
    //TODO: Publish a video resource
    playPosPct = Some(position)
    val a = avTransport.action("SetAVTransportURI").get
    val msg = createMessage(a, ("CurrentURI", location), ("CurrentURIMetaData", ""))
    sender.apply(controlUrl, msg).map {
      case _ => ()
    }
  }

  def setProperty(name: String, value: Any) = {
    debug("Setting property {} with value {} (not implemented).", name, if (value != null) value.toString else "null")
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
    playPosPct = None
    val hms = toDuration(position)
    val a = avTransport.action("Seek").get
    val msg = createMessage(a, ("Unit", "REL_TIME"), ("Target", hms))
    sender.apply(controlUrl, msg).map {
      case _ => ()
    }
  }

  def showPhoto(data: () => InputStream, length: Int, transition: String) = {
    dlnaPublisher.publishPhoto("tempid", data, length) match {
      case Some(url) =>
        info("Showing photo with length {}, transition {} is ignored.", length.toString, transition)
        val setAVTransportURI = avTransport.action("SetAVTransportURI").get
        val msg = createMessage(setAVTransportURI, ("CurrentURI", url), ("CurrentURIMetaData", ""))
        sender(controlUrl, msg).map {
          case _ => setRate(1.0)
        }

      case None =>
        throw new IOException("Failed to show photo because publishing failed.")
    }
  }

  def stop() = {
    val a = avTransport.action("Stop").get
    val msg = createMessage(a)
    sender.apply(controlUrl, msg).map {
      case _ => ()
    } recover {
      // My WDTVLIVE responds with 718 on Stop sometimes for no apparent reason...
      case x: SoapError if x.code == 718 => // ignore
    }
  }
}