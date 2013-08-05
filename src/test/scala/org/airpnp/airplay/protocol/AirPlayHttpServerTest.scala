package org.airpnp.airplay.protocol

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import org.airpnp.http.HttpTestUtil._
import org.testng.annotations.BeforeClass
import org.fest.assertions.Assertions.assertThat
import org.mockito.Mockito._
import org.mockito.Matchers._
import org.airpnp.airplay.AirPlayDevice
import java.net.InetSocketAddress
import org.airpnp.Util
import org.testng.annotations.BeforeMethod
import org.testng.annotations.AfterClass
import java.net.URLConnection
import org.airpnp.airplay.DurationAndPosition
import org.airpnp.http.HttpTestUtil
import org.testng.annotations.Test
import scala.xml.XML
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import org.mockito.Matchers
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

class AirPlayHttpServerTest {

  private var port: Int = 0
  private var server: AirPlayHttpServer = null
  private var apDevice: AirPlayDevice = null

  @BeforeClass def setup() = {
    apDevice = mock(classOf[AirPlayDevice])
    port = Util.findPort()
    server = new AirPlayHttpServer(new InetSocketAddress("localhost", port), apDevice)
    server.start()
  }

  @BeforeMethod def resetMock() = reset(apDevice)
  @AfterClass def cleanup() = server.stop(0)

  @Test
  def shouldQueryScrubAndPlayStatusWhenPlaybackInfoIsRequested(): Unit = {
    // Stub it to avoid NPE...
    stub(apDevice.getScrub).toReturn(future { new DurationAndPosition(0, 0) })
    stub(apDevice.isPlaying).toReturn(future { false })

    closeConn(openUrl("/playback-info"))

    verify(apDevice).getScrub
    verify(apDevice).isPlaying
  }

  @Test
  def shouldUseCorrectResponseContentTypeForPlaybackInfo(): Unit = {
    // Stub it to avoid NPE...
    stub(apDevice.getScrub).toReturn(future { new DurationAndPosition(0, 0) })
    stub(apDevice.isPlaying).toReturn(future { false })

    val conn = openUrl("/playback-info")
    conn.connect

    assertThat(conn.getContentType).isEqualTo("text/x-apple-plist+xml")

    closeConn(conn)
  }

  @Test
  def shouldCallStopWhenStopIsPosted(): Unit = {
    closeConn(postDataToUrl("/stop", port, null, new Array[Byte](0)))

    verify(apDevice).stop
  }

  @Test
  def shouldQueryScrubWhenScrubIsRequested(): Unit = {
    // Stub it to avoid NPE...
    stub(apDevice.getScrub).toReturn(future { new DurationAndPosition(0, 0) })

    closeConn(openUrl("/scrub"))

    verify(apDevice).getScrub
  }

  @Test
  def shouldReturnDurationAndPositionWhenScrubIsRequested(): Unit = {
    // Stub it to avoid NPE...
    stub(apDevice.getScrub).toReturn(future { new DurationAndPosition(2, 1) })

    val is = openUrlForReading("/scrub", port)
    val data = readAllAndClose(is)

    assertThat(data).isEqualTo("duration: 2.0\nposition: 1.0")
  }

  @Test
  def shouldCallScrubWhenScrubIsPosted(): Unit = {
    // Avoid NPE
    stub(apDevice.setScrub(anyDouble())).toReturn(future {})

    closeConn(postDataToUrl("/scrub?position=1.0", port, null, new Array[Byte](0)))

    verify(apDevice).setScrub(1.0)
  }

  @Test
  def shouldCallRateWhenRateIsPosted(): Unit = {
    // Avoid NPE
    stub(apDevice.setRate(anyDouble())).toReturn(future {})

    closeConn(postDataToUrl("/rate?value=1.0", port, null, new Array[Byte](0)))

    verify(apDevice).setRate(1.0)
  }

  @Test
  def shouldUseCorrectResponseContentTypeForServerInfo(): Unit = {
    val conn = openUrl("/server-info")
    conn.connect

    assertThat(conn.getContentType()).isEqualTo("text/x-apple-plist+xml")

    closeConn(conn)
  }

  @Test
  def shouldReturnCorrectDataForForServerInfo(): Unit = {
    stub(apDevice.getDeviceId).toReturn("fooid")

    val is = openUrlForReading("/server-info", port)
    val root = XML.load(is)
    is.close()

    // http://stackoverflow.com/questions/2803448/how-can-i-get-a-node-adjacent-to-a-unique-node-using-scala
    def findValue(xml: NodeSeq, key: String): Option[String] = {
      val components = xml \ "_"
      val index = components.zipWithIndex.find(_._1.text == key).map(_._2)
      index.map(_ + 1).map(components).map(_.text)
    }

    val result = findValue(root \\ "dict", "deviceid")

    assertThat(result).isEqualTo(Some("fooid"))
  }

  @Test
  def shouldCallPlayWhenPlayDataArePosted(): Unit = {
    // Avoid NPE
    stub(apDevice.play(anyString(), anyDouble())).toReturn(future {})

    val data = "Start-Position: 1.0\nContent-Location: http://localhost/test"
    closeConn(postDataToUrl("/play", port, null, data.getBytes))

    verify(apDevice).play("http://localhost/test", 1.0)
  }

  @Test
  def shouldCallPlayWhenPlayDataWithoutPositionArePosted(): Unit = {
    // Avoid NPE
    stub(apDevice.play(anyString(), anyDouble())).toReturn(future {})

    val data = "Content-Location: http://localhost/test"
    closeConn(postDataToUrl("/play", port, null, data.getBytes))

    verify(apDevice).play("http://localhost/test", 0.0)
  }

  @Test
  def shouldCallPlayWhenBinaryPlayDataArePosted(): Unit = {
    // Avoid NPE
    stub(apDevice.play(anyString(), anyDouble())).toReturn(future {})

    val is = getClass.getResourceAsStream("/org/airpnp/plist/airplay.bin")
    val data = readAllBytesAndClose(is)

    closeConn(postDataToUrl("/play", port, "application/x-apple-binary-plist", data))

    verify(apDevice).play(startsWith("http://v9.lscache4.googlevideo.com"),
      Matchers.eq(0.0005364880198612809d))
  }

  @Test
  def shouldCallSetPropertyWhenBinaryPropertyDataArePut(): Unit = {
    // Avoid NPE
    stub(apDevice.setProperty(anyString(), anyObject())).toReturn(future {})

    val bindata = "bplist00\u00d1\u0001\u0002Uvalue\u00d4\u0003\u0004\u0005\u0006\u0007\u0007\u0007\u0007YtimescaleUvalueUepochUflags\u0010\u0000\u0008\u000b\u0011\u001a$*06\u0000\u0000\u0000\u0000\u0000\u0000\u0001\u0001\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0008\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u00008".getBytes
    val conn = putDataToUrl("/setProperty?forwardEndTime", port, "application/x-apple-binary-plist", bindata)
    closeConn(conn)

    verify(apDevice).setProperty(Matchers.eq("forwardEndTime"), argThat(new BaseMatcher[Object]() {
      override def matches(arg0: Object) = arg0.asInstanceOf[Map[String, _]].contains("epoch")
      override def describeTo(arg0: Description) = arg0.appendText("map that contains the correct key")
    }))
  }

  //    @Test
  //    def shouldCallShowPhotoWhenPhotoIsPut(): Unit = {
  //        InputStream is = getClass().getResourceAsStream("pixel.jpg")
  //        byte[] photo = readAllBytesAndClose(is)
  //        
  //        URLConnection conn = putDataToUrl("/photo", port, "image/jpeg", photo)
  //        closeConn(conn)
  //        
  //        verify(apDevice).showPhoto(Mockito.eq(photo), Mockito.eq(""))
  //    }
  //    
  //    @Test
  //    def shouldUseTransitionHeaderWhenPhotoIsPut(): Unit = {
  //        InputStream is = getClass().getResourceAsStream("pixel.jpg")
  //        byte[] photo = readAllBytesAndClose(is)
  //        
  //        Map<String, String> headers = Collections.singletonMap("X-Apple-Transition", "test")
  //        URLConnection conn = HttpTestUtil.putDataToUrl("/photo", port, "image/jpeg", photo, headers)
  //        closeConn(conn)
  //        
  //        verify(apDevice).showPhoto(Mockito.eq(photo), Mockito.eq("test"))
  //    }
  //    
  private def openUrl(path: String) = openUrlConnection(path, port)
  private def closeConn(conn: URLConnection) = conn.getInputStream.close
}
