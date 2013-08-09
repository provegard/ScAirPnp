package org.airpnp.airplay.protocol

import org.airpnp.http.Response._
import scala.concurrent.ExecutionContext.Implicits.global
import org.airpnp.http.RouteHandler
import org.airpnp.http.Response
import org.airpnp.airplay.AirPlayDevice
import org.airpnp.http.Request
import org.airpnp.plist.PropertyList
import org.airpnp.plist.BinaryPropertyListDecoder
import org.airpnp.plist.Dict
import java.io.InputStream
import java.util.Properties
import java.io.InputStreamReader
import org.airpnp.Util
import scala.util.Success
import scala.util.Failure
import java.io.ByteArrayInputStream

private[protocol] object RouteHelper {
  def internalServerError(response: Response, t: Throwable) = {
    val msg = t.getMessage match {
      case s if s != null => s
      case _ => "Unknown error"
    }
    response.respond(withText(msg).andStatusCode(500))
  }
}

class PhotoRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePUT(request: Request, response: Response) = {
    val transList = request.getHeader("X-Apple-Transition")
    val transition = transList.headOption.getOrElse("")

    val length = request.getHeader("Content-Length").headOption.getOrElse("0").toInt
    
    //TODO: For now, we consume all data and then publish streams off of it. In the 
    // future, do these two things concurrently!
    val data = Util.readAllBytes(request.getInputStream)

    apDevice.showPhoto(new ByteArrayInputStream(data), length, transition).onComplete {
      case Success(_) => response.respond(withStatus(200))
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class PlaybackInfoRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handleGET(request: Request, response: Response) = {

    val fScrub = apDevice.getScrub
    val fPlaying = apDevice.isPlaying
    val waiting = for {
      scrub <- fScrub
      isPlaying <- fPlaying
    } yield (scrub, isPlaying)

    waiting.onComplete {
      case Success(result) => {
        val pi = new PlaybackInfo(result._1, result._2)
        val plist = pi.get
        response.respond(withUtf8Text(plist.toXml).andContentType(PropertyList.CT_TEXT_PLIST))
      }
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class PlayRoute(private val apDevice: AirPlayDevice) extends RouteHandler {

  override def handlePOST(request: Request, response: Response) = {
    //TODO: error handling
    val ct = request.getHeader("Content-Type").head
    val (loc, pos) = ct match {
      case PropertyList.CT_BINARY_PLIST => readBinaryPropertyList(request.getInputStream)
      case _ => readTextValues(request.getInputStream)
    }
    apDevice.play(loc, pos).onComplete {
      case Success(_) => response.respond(withStatus(200))
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }

  private def readBinaryPropertyList(inputStream: InputStream): (String, Double) = {
    val decoder = new BinaryPropertyListDecoder(inputStream)
    val plist = decoder.decode
    val d = plist.root.asInstanceOf[Dict].getValue()

    val loc = d.get("Content-Location").get.toString
    val pos = d.get("Start-Position").get.asInstanceOf[Double]

    (loc, pos)
  }

  private def readTextValues(inputStream: InputStream) = {
    val props = new Properties
    props.load(new InputStreamReader(inputStream, "ASCII"))

    val loc = props.get("Content-Location").toString
    val startPos = props.get("Start-Position")
    val pos = if (startPos != null) java.lang.Double.parseDouble(startPos.toString()) else 0.0d

    (loc, pos)
  }
}

class RateRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePOST(request: Request, response: Response) = {
    //TODO: error checking
    val value = request.getArgument("value").head
    apDevice.setRate(java.lang.Double.parseDouble(value)).onComplete {
      case Success(_) => response.respond(withStatus(200))
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class ScrubRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handleGET(request: Request, response: Response) = {
    apDevice.getScrub.onComplete {
      case Success(scrub) => {
        val data = "duration: " + scrub.duration + "\nposition: " + scrub.position
        response.respond(withText(data))
      }
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }

  override def handlePOST(request: Request, response: Response) = {
    //TODO: error checking
    val pos = request.getArgument("position").head
    apDevice.setScrub(java.lang.Double.parseDouble(pos)).onComplete {
      case Success(_) => response.respond(withStatus(200))
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class ServerInfoRoute(private val apDevice: AirPlayDevice) extends RouteHandler {

  override def handleGET(request: Request, response: Response) = {
    //TODO: We can extract X-apple-client-name here
    val si = new ServerInfo(apDevice.getDeviceId, apDevice.getFeatures, apDevice.getModel)
    val plist = si.get
    response.respond(withUtf8Text(plist.toXml).andContentType(PropertyList.CT_TEXT_PLIST))
  }
}

class SetPropertyRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePUT(request: Request, response: Response) = {
    //TODO: Check CT + error handling
    val decoder = new BinaryPropertyListDecoder(request.getInputStream)
    val plist = decoder.decode

    val propName = request.getArgument("").head // unnamed arg

    val map = plist.root.asInstanceOf[Dict].getValue
    apDevice.setProperty(propName, map.get("value").get).onComplete {
      case Success(_) => response.respond(withStatus(200))
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class SlideshowFeaturesRoute extends RouteHandler {
  override def handleGET(request: Request, response: Response) = {
    val sf = new SlideshowFeatures
    val plist = sf.get
    response.respond(withUtf8Text(plist.toXml).andContentType(PropertyList.CT_TEXT_PLIST))
  }
}

class StopRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePOST(request: Request, response: Response) = {
    apDevice.stop()
    response.respond(withStatus(200))
  }
}

class ReverseRoute(apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePOST(request: Request, response: Response) {
    //Upgrade: PTTH/1.0
    //X-apple-device-id: 0x............
    //Content-length: 0
    //Connection: Upgrade
    //X-apple-purpose: event
    //User-agent: AirPlay/160.10 (Photos)
    //X-apple-session-id: <guid>
    //X-apple-client-name: ...
    //NOTE: We cannot do anything with the connection, because Oracle's HTTP
    // server doesn't allow us to do anything beyond responding with 101. If
    // we only could get hold of the socket channel...
    response.dontClose()
    response.addHeader("Upgrade", "PTTH/1.0")
    response.addHeader("Connection", "Upgrade")
    response.respond(withStatus(101))
  }
}