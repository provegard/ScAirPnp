package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import scala.xml.XML

class MediaRendererDeviceTest {
  private var device: Device = null

  @BeforeClass
  def createDevice(): Unit = {
    val stream = getClass.getResourceAsStream("mediarenderer/device_root.xml")
    val elem = XML.load(stream)
    device = new Device(elem, "http://www.base.com")
  }

  @Test
  def shouldHaveBaseUrl(): Unit = {
    assertThat(device.getBaseUrl).isEqualTo("http://www.base.com")
  }

  @Test
  def shouldExposeFriendlyName(): Unit = {
    assertThat(device.getFriendlyName).isEqualTo("WDTVLIVE")
  }

  @Test
  def shouldExposeUDN(): Unit = {
    assertThat(device.getUdn).isEqualTo("uuid:67ff722f-0090-a976-17db-e9396986c234")
  }

  //    @Test
  //    def shouldReadAttributesWithNamespace(): Unit = {
  //        assertThat(device.attr("X_DLNADOC")).isEqualTo("DMR-1.50")
  //    }
  //    
  @Test
  def shouldHaveProperStringRepresentation(): Unit = {
    assertThat(device.toString).isEqualTo("WDTVLIVE [UDN=uuid:67ff722f-0090-a976-17db-e9396986c234]")
  }

  @Test
  def shouldHaveServices(): Unit = {
    assertThat(device.getServices.size).isEqualTo(3)
  }

  @Test
  def shouldLookupServiceById(): Unit = {
    val s = device.getServiceById("urn:upnp-org:serviceId:AVTransport")
    assertThat(s).isNotNull()
  }
  
  @Test
  def shouldBeMediaRenderer(): Unit = {
    assertThat(device.isMediaRenderer).isTrue
  }
}