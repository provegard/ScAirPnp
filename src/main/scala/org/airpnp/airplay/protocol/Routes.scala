package org.airpnp.airplay.protocol

import scala.concurrent.ExecutionContext.Implicits.global;
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

private[protocol] object RouteHelper {
  def internalServerError(response: Response, t: Throwable) = {
    val msg = t.getMessage match {
      case s if s != null => s
      case _ => "Unknown error"
    }
    response.respond(msg, 500)
  }
}

class PhotoRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePUT(request: Request, response: Response) = {
    val transList = request.getHeader("X-Apple-Transition")
    val transition = transList.headOption.getOrElse("")

    apDevice.showPhoto(request.getInputStream, transition).onComplete {
      case Success(_) => response.respond("")
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
        response.respondRaw(plist.toXml.getBytes, contentType = PropertyList.CT_TEXT_PLIST)
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
      case Success(_) => response.respond("")
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
      case Success(_) => response.respond("")
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class ScrubRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handleGET(request: Request, response: Response) = {
    apDevice.getScrub.onComplete {
      case Success(scrub) => {
        val data = "duration: " + scrub.duration + "\nposition: " + scrub.position
        response.respond(data)
      }
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }

  override def handlePOST(request: Request, response: Response) = {
    //TODO: error checking
    val pos = request.getArgument("position").head
    apDevice.setScrub(java.lang.Double.parseDouble(pos)).onComplete {
      case Success(_) => response.respond("")
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class ServerInfoRoute(private val apDevice: AirPlayDevice) extends RouteHandler {

  override def handleGET(request: Request, response: Response) = {
    val si = new ServerInfo(apDevice.getDeviceId, apDevice.getFeatures, apDevice.getModel)
    val plist = si.get
    response.respondRaw(plist.toXml.getBytes, contentType = PropertyList.CT_TEXT_PLIST)
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
      case Success(_) => response.respond("")
      case Failure(t) => RouteHelper.internalServerError(response, t)
    }
  }
}

class SlideshowFeaturesRoute extends RouteHandler {
  override def handleGET(request: Request, response: Response) = {
    val sf = new SlideshowFeatures
    val plist = sf.get
    response.respondRaw(plist.toXml.getBytes, contentType = PropertyList.CT_TEXT_PLIST)
  }
}

class StopRoute(private val apDevice: AirPlayDevice) extends RouteHandler {
  override def handlePOST(request: Request, response: Response) = {
    apDevice.stop()
    response.respond("")
  }
}