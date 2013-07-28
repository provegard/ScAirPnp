package org.airpnp.upnp

import scala.xml.XML

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class PrinterDeviceTest {
  private var device: Device = null

  @BeforeClass
  def createDevice(): Unit = {
    val stream = getClass.getResourceAsStream("printer/root.xml")
    val elem = XML.load(stream)
    device = new Device(elem, "http://www.base.com")
  }

  @Test
  def shouldNotBeMediaRenderer(): Unit = {
    assertThat(device.isMediaRenderer).isFalse
  }
}