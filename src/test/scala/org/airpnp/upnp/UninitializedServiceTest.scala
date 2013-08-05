package org.airpnp.upnp

import scala.collection.JavaConversions._
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import scala.xml.XML
import org.testng.annotations.DataProvider

class UninitializedServiceTest {
  private var service: Service = null

  @BeforeClass
  def createService() {
    var stream = getClass.getResourceAsStream("mediarenderer/root.xml")
    var root = XML.load(stream)
    val device = new Device(root, "http://www.base.com")

    service = device.getServiceById("urn:upnp-org:serviceId:AVTransport").get
  }

  @Test
  def shouldNotHaveActions() {
    val a = service.action("GetCurrentTransportActions")
    assertThat(a).isEqualTo(None)
  }

}