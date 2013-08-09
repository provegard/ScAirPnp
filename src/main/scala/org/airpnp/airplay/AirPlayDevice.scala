package org.airpnp.airplay;

import java.io.InputStream
import scala.concurrent.Future

class DurationAndPosition(val duration: Double, val position: Double)

abstract class AirPlayDevice {
  def getModel(): String
  def getDeviceId(): String
  def getFeatures(): Int
  def getScrub(): Future[DurationAndPosition]
  def isPlaying(): Future[Boolean]
  def setScrub(position: Double): Future[Unit]
  def play(location: String, position: Double): Future[Unit]
  def stop(): Future[Unit]
  def showPhoto(data: InputStream, length: Int, transition: String): Future[Unit]
  def setRate(rate: Double): Future[Unit]
  def setProperty(name: String, value: Any): Future[Unit]
  def getName(): String
}
