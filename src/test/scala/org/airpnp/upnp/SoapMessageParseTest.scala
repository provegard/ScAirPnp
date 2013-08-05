package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import org.testng.annotations.BeforeClass

class SoapMessageParseTest {
  private var message: SoapMessage = null

  @BeforeClass
  def createMessage() {
    val orig = new SoapMessage("type", "action")
    orig.setArgument("foo", "bar\u1234")
    var is = new ByteArrayInputStream(orig.toString.getBytes("UTF-8"))
    message = SoapMessage.parse(is)
  }

  @Test
  def shouldParseTheAName() {
    assertThat(message.getName).isEqualTo("action")
  }

  @Test
  def shouldParseServiceType() {
    assertThat(message.getHeader).isEqualTo("type#action")
  }

  @Test
  def shouldParseArguments() {
    assertThat(message.getArgument("foo", "")).isEqualTo("bar\u1234")
  }

}