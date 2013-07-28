package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

class MSearchRequestTest {
  private var message: UPnPMessage = null

  @BeforeClass
  def parseRequest() = {
    message = new UPnPMessage(new MSearchRequest("ssdp:all", 5).toString)
  }

  @Test def shouldHaveMSearchMethod(): Unit = {
    assertThat(message.getMethod).isEqualTo("M-SEARCH")
  }

  @Test def shouldBeDiscoveryMessage(): Unit = {
    val man = message.getHeaders.get("MAN")
    assertThat(man).isEqualTo(Some("\"ssdp:discover\""))
  }

  @Test def shouldHaveCorrectSTValue(): Unit = {
    val man = message.getHeaders.get("ST")
    assertThat(man).isEqualTo(Some("ssdp:all"))
  }

  @Test def shouldHaveCorrectMXValue(): Unit = {
    val man = message.getHeaders.get("MX")
    assertThat(man).isEqualTo(Some("5"))
  }

  @Test def shouldHaveCorrectHost(): Unit = {
    val man = message.getHeaders.get("HOST")
    assertThat(man).isEqualTo(Some("239.255.255.250:1900"))
  }
}