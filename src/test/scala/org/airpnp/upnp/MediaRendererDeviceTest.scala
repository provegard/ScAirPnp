package org.airpnp.upnp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import scala.xml.XML

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class MediaRendererDeviceTest {
  private var device: Device = null

  @BeforeMethod def createDevice() {
    val stream = getClass.getResourceAsStream("mediarenderer/root.xml")
    val elem = XML.load(stream)
    device = new Device(elem, "http://www.base.com")
  }

  @Test
  def shouldHaveBaseUrl() {
    assertThat(device.getBaseUrl).isEqualTo("http://www.base.com")
  }

  @Test
  def shouldExposeFriendlyName() {
    assertThat(device.getFriendlyName).isEqualTo("WDTVLIVE")
  }

  @Test
  def shouldExposeUDN() {
    assertThat(device.getUdn).isEqualTo("uuid:67ff722f-0090-a976-17db-e9396986c234")
  }

  //    @Test
  //    def shouldReadAttributesWithNamespace() {
  //        assertThat(device.attr("X_DLNADOC")).isEqualTo("DMR-1.50")
  //    }
  //    
  @Test
  def shouldHaveProperStringRepresentation() {
    assertThat(device.toString).isEqualTo("WDTVLIVE [UDN=uuid:67ff722f-0090-a976-17db-e9396986c234]")
  }

  @Test
  def shouldHaveServices() {
    assertThat(device.getServices.size).isEqualTo(3)
  }

  @Test
  def shouldLookupServiceById() {
    val s = device.getServiceById("urn:upnp-org:serviceId:AVTransport")
    assertThat(s).isNotNull()
  }

  @Test
  def shouldBeMediaRenderer() {
    assertThat(device.isMediaRenderer).isTrue
  }

  @Test def shouldAllowSettingSoapSender() {
    val soapSender: Device.SoapSender = (url, msg) => future { null }
    device.soapSender = soapSender
    assertThat(device.soapSender).isSameAs(soapSender)
  }

  @Test(expectedExceptions = Array(classOf[IllegalStateException]))
  def shouldNotAllowSettingSoapSenderTwice() {
    val soapSender: Device.SoapSender = (url, msg) => future { null }
    device.soapSender = soapSender
    device.soapSender = soapSender
  }

  @Test def shouldAllowClearingSoapSender() {
    val soapSender: Device.SoapSender = (url, msg) => future { null }
    device.soapSender = soapSender
    device.soapSender = null
    assertThat(device.soapSender).isNull()
  }

}