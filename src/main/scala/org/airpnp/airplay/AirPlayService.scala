package org.airpnp.airplay;

import scala.language.implicitConversions
import scala.collection.JavaConversions._
import javax.jmdns.ServiceInfo;

class AirPlayService(private val device: AirPlayDevice, private val port: Int) {

  private val service = {
    val serviceType = "_airplay._tcp.local."
    val features = "0x" + Integer.toHexString(device.getFeatures())

    val properties = Map(("deviceid", device.getDeviceId().getBytes()),
      ("features", features.getBytes()),
      ("model", device.getModel().getBytes()))

    ServiceInfo.create(serviceType, device.getName(), port, 0, 0, true, properties)
  }

  def getService() = service
}
