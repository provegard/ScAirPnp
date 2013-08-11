package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class UninitializedServiceTest {
  private var service: Service = null

  @BeforeClass
  def createService() {
    val device = buildUninitializedMediaRenderer("http://www.base.com")

    service = device.getServiceById("urn:upnp-org:serviceId:AVTransport").get
  }

  @Test
  def shouldNotHaveActions() {
    val a = service.action("GetCurrentTransportActions")
    assertThat(a).isEqualTo(None)
  }

}