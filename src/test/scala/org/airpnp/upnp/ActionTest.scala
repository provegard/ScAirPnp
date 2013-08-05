package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import scala.xml.XML

class ActionTest {
  private var action: Action = null

  @BeforeClass
  def createAction() {
    val deviceStream = getClass.getResourceAsStream("mediarenderer/root.xml")
    val device = new Device(XML.load(deviceStream), "http://base.com")
    val service = device.getServices.head

    val serviceStream = getClass.getResourceAsStream("mediarenderer/service_scpd.xml")
    val root = XML.load(serviceStream)
    service.initialize(root)

    val a = (root \\ "actionList" \ "action")(0)
    action = new Action(a, service)
  }

  @Test def shouldBeAbleToCreateASoapMessage() {
    val msg = action.createSoapMessage(("InstanceID", "0"))
    assertThat(msg.getHeader).isEqualTo("urn:schemas-upnp-org:service:AVTransport:1#GetCurrentTransportActions")
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def shouldRequireInArgsWhenCreatingASoapMessage() {
    action.createSoapMessage()
  }

  @Test
  def shouldHaveAName() {
    assertThat(action.getName).isEqualTo("GetCurrentTransportActions")
  }

  @Test
  def shouldHaveInputArguments() {
    assertThat(action.inputArguments).isEqualTo(Seq("InstanceID"))
  }

  @Test
  def shouldHaveOutputArguments() {
    assertThat(action.outputArguments).isEqualTo(Seq("Actions"))
  }
}