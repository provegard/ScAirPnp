package org.airpnp.actor

import scala.collection.JavaConverters._
import org.mockito.Mockito.mock
import org.airpnp.http.Response._
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import scala.actors.Actor
import scala.collection.mutable.MutableList
import scala.xml.XML
import org.airpnp.Util
import org.airpnp.airplay.MDnsServiceHost
import org.airpnp.http.HttpTestUtil
import org.airpnp.http.Request
import org.airpnp.http.Response
import org.airpnp.http.RouteHandler
import org.airpnp.http.RoutingHttpServer
import org.airpnp.upnp.Device
import org.airpnp.upnp.SoapMessage
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.Test
import javax.jmdns.ServiceInfo
import org.airpnp.dlna.DLNAPublisher
import org.airpnp.TraceLogging
import org.testng.Assert

class DevicePublisherTest extends TraceLogging {
  private def createDevice(baseUrl: String) = {
    var is = getClass.getResourceAsStream("/org/airpnp/upnp/mediarenderer/root.xml")
    val device = new Device(XML.load(is), baseUrl)

    for (s <- device.getServices) {
      val urlParts = s.getSCPDURL.split("/")
      val file = urlParts(urlParts.length - 1)
      is = getClass.getResourceAsStream("/org/airpnp/upnp/mediarenderer/" + file)
      s.initialize(XML.load(is))
    }

    device
  }

  @Test def testInteractionWithPublishedDevice() {
    val devicePort = Util.findPort()
    val url = "http://localhost:" + devicePort
    val device = createDevice(url)
    val deviceServer = new DevicePublisherTest.SoapServer(new InetSocketAddress(InetAddress.getByName("localhost"), devicePort))

    val host = new DevicePublisherTest.FakeHost()
    val addr = InetAddress.getByName("localhost")
    val publisher = new DevicePublisher(host, addr, mock(classOf[DLNAPublisher]))

    try {
      deviceServer.start()
      publisher.start()

      // 1. Publish the device and find out the port for the AirPlay server.
      publisher ! Publish(device)
      waitForRegistration(host)

      val service = host.services.head
      val airPlayPort = service.getPort()
      val airPlayUrl = "http://localhost:" + airPlayPort

      val is = new URL(airPlayUrl + "/playback-info").openStream()
      val plist = XML.load(is)
      is.close()

      val firstReal = (plist \\ "real")(0).text
      assertThat(firstReal).isEqualTo("59.0")

    } finally {
      if (publisher.getState == Actor.State.Runnable) {
        publisher !? Stop
      }
      deviceServer.stop(0)
    }
  }

  @Test def testQueryingPublishedDevices() {
    val devicePort = Util.findPort()
    val url = "http://localhost:" + devicePort
    val device = createDevice(url)
    val deviceServer = new DevicePublisherTest.SoapServer(new InetSocketAddress(InetAddress.getByName("localhost"), devicePort))

    val host = new DevicePublisherTest.FakeHost()
    val addr = InetAddress.getByName("localhost")
    val publisher = new DevicePublisher(host, addr, mock(classOf[DLNAPublisher]))

    try {
      deviceServer.start()
      publisher.start()

      // 1. Publish the device and find out the port for the AirPlay server.
      publisher ! Publish(device)
      waitForRegistration(host)

      publisher !? (2000, GetPublishedDevices()) match {
        case Some(msg) => msg match {
          case GetPublishedDevicesReply(devices) =>
            assertThat(devices.asJava).contains(device)
          case _ => Assert.fail("Didn't get GetPublishedDevicesReply")
        }
        case None => Assert.fail("Didn't get GetPublishedDevices reply in time")
      }
    } finally {
      if (publisher.getState == Actor.State.Runnable) {
        publisher !? Stop
      }
      deviceServer.stop(0)
    }
  }

  private def waitForRegistration(host: DevicePublisherTest.FakeHost) {
    if (!host.registered.await(2000, TimeUnit.MILLISECONDS)) {
      throw new IllegalStateException("Publishing failed")
    }
  }
}

object DevicePublisherTest {
  private class FakeHost extends MDnsServiceHost {
    val registered = new CountDownLatch(1)
    val services = new MutableList[ServiceInfo]()
    def start(addr: InetAddress) {
    }
    def stop() {
    }
    def register(service: ServiceInfo) {
      services += service
      registered.countDown()
    }
    def unregister(service: ServiceInfo) {
    }
  }

  private class ControlHandler extends RouteHandler {
    override def handlePOST(request: Request, response: Response) {
      val msg = SoapMessage.parse(request.getInputStream)
      msg.getName match {
        case "GetPositionInfo" => {
          val reply = new SoapMessage(msg.getServiceType, "GetPositionInfoReply")
          reply.setArgument("TrackDuration", "0:00:59")
          reply.setArgument("RelTime", "0:00:00")
          response.respond(withUtf8Text(reply.toString).andContentType("text/xml"))
        }
        case "GetTransportInfo" => {
          val reply = new SoapMessage(msg.getServiceType, "GetTransportInfoReply")
          reply.setArgument("CurrentTransportState", "PLAYING")
          response.respond(withUtf8Text(reply.toString).andContentType("text/xml"))
        }
        case _ => response.respond(withStatus(501))
      }
    }
  }

  private class SoapServer(private val addr: InetSocketAddress) extends RoutingHttpServer(addr) {
    addRoute("/MediaRenderer_AVTransport/control", new ControlHandler())
  }
}