package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass

class UpnpMessageTest {
  class MSearchMessageTest {
    private val data = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\n" +
      "MX: 1\r\nST: urn:schemas-upnp-org:device:ZonePlayer:1\r\n\r\n"

    @Test def shouldIgnoreTrailingWhitespaceAfterHeader(): Unit = {
      val message = new UPnPMessage(data + "   ")
      assertThat(message.isNotification).isFalse()
    }

    @Test def shouldExposeHeaders(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getHeaders.size).isEqualTo(4)
    }

    @Test def shouldHaveNoUdn(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getUdn).isEqualTo(None)
    }

    @Test def shouldHaveMSearchMethod(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getMethod).isEqualTo("M-SEARCH")
    }

    @Test def shouldNotBeNotification(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.isNotification).isFalse()
    }

    @Test def shouldHaveNoNotificationSubType(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getNotificationSubType).isEqualTo(None)
    }

    @Test def shouldHaveNoLocation(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getLocation).isEqualTo(None);
    }

    @Test def shouldHaveNoType(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getType).isEqualTo(None);

    }
  }

  class NotifyMessageTest {
    private val data = "NOTIFY * HTTP/1.1\r\n" +
      "Host:239.255.255.250:1900\r\n" +
      "NT:upnp:rootdevice\r\n" +
      "NTS:ssdp:alive\r\n" +
      "Location:http://172.16.0.25:2869/upnphost/udhisapi.dll?content=uuid:17a96d54-d51d-44ee-919e-337163d78eae\r\n" +
      "USN:uuid:17a96d54-d51d-44ee-919e-337163d78eae::upnp:rootdevice\r\n" +
      "Cache-Control:max-age=900\r\n" +
      "Server:Microsoft-Windows-NT/5.1 UPnP/1.0 UPnP-Device-Host/1.0\r\n" +
      "OPT:\"http://schemas.upnp.org/upnp/1/0/\";.ns=01\r\n" +
      "01-NLS:8a73f9e4f21bd13812797b793bcf7357\r\n\r\n";

    @Test def shouldBeNotification(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.isNotification).isTrue()
    }

    @Test def shouldHaveCorrectUdn(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getUdn).isEqualTo(Some("uuid:17a96d54-d51d-44ee-919e-337163d78eae"));
    }

    @Test def shouldHaveCorrectLocation(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getLocation).isEqualTo(Some("http://172.16.0.25:2869/upnphost/udhisapi.dll?content=uuid:17a96d54-d51d-44ee-919e-337163d78eae"));
    }

    @Test def shouldBeAlive(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.isAlive).isTrue()
    }

    @Test def shouldNotBeByeBye(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.isByeBye).isFalse()
    }

    @Test def shouldBeByeByeIfNTSIsChanged(): Unit = {
      val message = new UPnPMessage(data.replace("ssdp:alive", "ssdp:byebye"))
      assertThat(message.isByeBye).isTrue()
    }

    @Test def shouldNotBeAliveIfNTSIsChanged(): Unit = {
      val message = new UPnPMessage(data.replace("ssdp:alive", "ssdp:byebye"))
      assertThat(message.isAlive).isFalse()
    }

    @Test def shouldHaveTheCorrectType(): Unit = {
      val message = new UPnPMessage(data)
      assertThat(message.getType).isEqualTo(Some("upnp:rootdevice"));
    }
  }
}
