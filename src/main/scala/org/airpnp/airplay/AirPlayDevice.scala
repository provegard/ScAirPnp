package org.airpnp.airplay;

import java.io.InputStream

class DurationAndPosition(val duration: Double, val position: Double)

abstract class AirPlayDevice {
  def getModel(): String
  def getDeviceId(): String
  def getFeatures(): Int
  def getScrub(): DurationAndPosition
  def isPlaying(): Boolean
  def setScrub(position: Double)
  def play(location: String, position: Double)
  def stop()
  def showPhoto(data: InputStream, transition: String)
  def setRate(rate: Double)
  def setProperty(name: String, value: AnyRef)
  def getName(): String
}
