package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.ByteArrayInputStream
import org.testng.annotations.BeforeClass

class SoapErrorParseTest {
  private var error: SoapError = null
  private val xml = "<?xml version=\"1.0\"?>" +
    "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
    "    s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
    "  <s:Body>" +
    "    <s:Fault>" +
    "      <faultcode>s:Client</faultcode>" +
    "      <faultstring>UPnPError</faultstring>" +
    "      <detail>" +
    "        <UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">" +
    "          <errorCode>718</errorCode>" +
    "          <errorDescription>That is 718</errorDescription>" +
    "        </UPnPError>" +
    "      </detail>" +
    "    </s:Fault>" +
    "  </s:Body>" +
    "</s:Envelope>"

  @BeforeClass
  def setup() {
    error = SoapError.parse(new ByteArrayInputStream(xml.getBytes("UTF8")))
  }

  @Test def shouldExtractCode() {
    assertThat(error.code).isEqualTo(718)
  }

  @Test def shouldExtractDescription() {
    assertThat(error.description).isEqualTo("That is 718")
  }

  @Test def testThatSoapMessageCanParseAndTellThatItIsFault() {
    val msg = SoapMessage.parse(new ByteArrayInputStream(xml.getBytes("UTF8")))
    assertThat(msg.isFault).isTrue
  }
}