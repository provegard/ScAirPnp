package org.airpnp.airplay;

import org.airpnp.Util;

/**
 * Base implementation of the {@link AirPlayDevice} interface, providing model,
 * device ID, features and name.
 */
abstract class BaseAirPlayDevice(private val name: String, private val udn: String) extends AirPlayDevice {

  private val id = Util.createDeviceId(udn)

  override def getModel() = "AppleTV2,1"
  override def getDeviceId() = id

  // 0x77 instead of 0x07 in order to support AirPlay from ordinary apps;
  // also means that the body for play will be a binary plist.
  override def getFeatures() = 0x77

  override def getName() = name
}
