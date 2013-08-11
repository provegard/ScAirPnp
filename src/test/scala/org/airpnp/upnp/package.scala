package org.airpnp

import scala.xml.XML

package object upnp {

  def buildUninitializedMediaRenderer(baseUrl: String) = {
    val deviceStream = getClass.getResourceAsStream("/org/airpnp/upnp/mediarenderer/root.xml")
    new Device(XML.load(deviceStream), baseUrl)
  }

  def buildInitializedMediaRenderer(baseUrl: String) = {
    val device = buildUninitializedMediaRenderer(baseUrl)

    for (s <- device.getServices) {
      val url = s.getSCPDURL
      val name = url.substring(url.lastIndexOf("/") + 1)
      val serviceStream = getClass.getResourceAsStream("/org/airpnp/upnp/mediarenderer/" + name)
      s.initialize(XML.load(serviceStream))

    }

    device
  }
}