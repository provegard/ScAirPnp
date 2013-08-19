package org.airpnp

import scala.xml.XML
import java.io.StringWriter
import scala.xml.MinimizeMode
import java.io.ByteArrayInputStream

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

  def createSoapError(code: Int, message: String): SoapError = {
    val doc = <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                  <s:Fault>
                    <faultcode>s:Client</faultcode>
                    <faultstring>UPnPError</faultstring>
                    <detail>
                      <UPnPError xmlns="urn:schemas-upnp-org:control-1-0">
                        <errorCode>{ code }</errorCode>
                        <errorDescription>{ message }</errorDescription>
                      </UPnPError>
                    </detail>
                  </s:Fault>
                </s:Body>
              </s:Envelope>
    val sw = new StringWriter
    XML.write(sw, doc, "UTF-8", true, null, MinimizeMode.Default)
    SoapError.parse(new ByteArrayInputStream(sw.toString.getBytes("UTF8")))
  }
}