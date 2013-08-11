package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class ActionTest {
  private var action: Action = null

  @BeforeClass
  def createAction() {
    val device = buildInitializedMediaRenderer("http://base.com")
    action = device.getServices.head.action("GetCurrentTransportActions").get
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