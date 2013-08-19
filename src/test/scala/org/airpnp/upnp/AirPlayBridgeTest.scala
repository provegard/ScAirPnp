package org.airpnp.upnp

import org.airpnp.Util.combinefuture
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import org.mockito.Mockito.{ mock, when, verify, never }
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.airpnp.dlna.DLNAPublisher
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import org.mockito.Matchers.{ isA, anyString, argThat, any => mockAny, anyInt }
import org.mockito.Matchers.{ eq => mockEq }
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.hamcrest.Matcher
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.fest.assertions.Assertions.assertThat
import org.airpnp.airplay.DurationAndPosition
import java.io.InputStream
import org.airpnp.TraceLogging
import java.io.IOException
import org.mockito.Mockito

private abstract class FakeSender {
  def send(url: String, msg: SoapMessage): Future[SoapMessage]
}

class AirPlayBridgeTest extends TraceLogging {
  type InputStreamFactory = () => InputStream

  private var device: Device = null
  private var bridge: AirPlayBridge = null
  private var publisher: DLNAPublisher = null

  private var fakeSender: FakeSender = null

  @BeforeClass
  def setup() {
    device = buildInitializedMediaRenderer("http://base.com")
  }

  @BeforeMethod
  def initBridge() {
    fakeSender = mock(classOf[FakeSender])
    publisher = mock(classOf[DLNAPublisher])

    val sender = (url: String, msg: SoapMessage) => fakeSender.send(url, msg)
    device.soapSender = null // clear first
    device.soapSender = sender

    bridge = new AirPlayBridge(device, publisher)
  }

  @Test def soapSenderShouldGetControlUrl() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("TrackDuration", "0:00:59"), ("RelTime", "0:00:00"))
    }))
    Await.result(bridge.getScrub, 1 second)
    verify(fakeSender).send(mockEq("http://base.com/MediaRenderer_AVTransport/control"), isA(classOf[SoapMessage]))
  }

  @Test def getScrubShouldSendGetPositionInfo() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("TrackDuration", "0:00:59"), ("RelTime", "0:00:00"))
    }))
    Await.result(bridge.getScrub, 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("GetPositionInfo"))
  }

  @Test def getScrubShouldDecodeDurationAndPosition() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("TrackDuration", "0:00:59"), ("RelTime", "0:00:10"))
    }))
    val dp = Await.result(bridge.getScrub, 1 second)
    assertThat(dp).isEqualTo(DurationAndPosition(59.0, 10.0))
  }

  @Test def getScrubShouldSeekAfterPlayIfItHasDuration() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg =>
        msg.getName match {
          case "SetAVTransportURI" => createReply(msg)
          case "Seek" => createReply(msg)
          case "GetPositionInfo" => createReply(msg, ("TrackDuration", "0:01:00"), ("RelTime", "0:00:00"))
        }
    }))
    val f = bridge.play("http://foo.com/some.mpg", 0.1).followWith(bridge.getScrub)
    Await.result(f, 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("SetAVTransportURI"))
    verify(fakeSender).send(anyString, soapMessageWithName("GetPositionInfo"))
    verify(fakeSender).send(anyString, soapMessageWithName("Seek"))
  }

  @Test def getScrubShouldSeekWithPlayPositionAsPercentage() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg =>
        msg.getName match {
          case "SetAVTransportURI" => createReply(msg)
          case "Seek" => createReply(msg)
          case "GetPositionInfo" => createReply(msg, ("TrackDuration", "0:01:00"), ("RelTime", "0:00:00"))
        }
    }))
    val f = bridge.play("http://foo.com/some.mpg", 0.1).followWith(bridge.getScrub)
    Await.result(f, 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("SetAVTransportURI"))
    verify(fakeSender).send(anyString, soapMessageWithName("GetPositionInfo"))
    verify(fakeSender).send(anyString, soapMessageWithArgument("Target", "0:00:06.000"))
  }

  @Test def getScrubShouldNotSeekAfterPlayIfItDoesntHaveDuration() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg =>
        msg.getName match {
          case "SetAVTransportURI" => createReply(msg)
          case "Seek" => createReply(msg)
          case "GetPositionInfo" => createReply(msg, ("TrackDuration", "0:00:00"), ("RelTime", "0:00:00"))
        }
    }))
    val f = bridge.play("http://foo.com/some.mpg", 0.1).followWith(bridge.getScrub)
    Await.result(f, 1 second)
    verify(fakeSender, never()).send(anyString, soapMessageWithName("Seek"))
  }

  @Test def getScrubShouldNotSeekAfterPlayIfPositionHasBeenPassedAlready() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg =>
        msg.getName match {
          case "SetAVTransportURI" => createReply(msg)
          case "Seek" => createReply(msg)
          case "GetPositionInfo" => createReply(msg, ("TrackDuration", "0:01:00"), ("RelTime", "0:00:10"))
        }
    }))
    val f = bridge.play("http://foo.com/some.mpg", 0.1).followWith(bridge.getScrub)
    Await.result(f, 1 second)
    verify(fakeSender, never()).send(anyString, soapMessageWithName("Seek"))
  }

  @Test def getScrubShouldNotSeekAfterManualSeek() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg =>
        msg.getName match {
          case "SetAVTransportURI" => createReply(msg)
          case "Seek" => createReply(msg)
          case "GetPositionInfo" => createReply(msg, ("TrackDuration", "0:01:00"), ("RelTime", "0:00:00"))
        }
    }))
    val f = bridge.play("http://foo.com/some.mpg", 0.1).followWith(bridge.setScrub(1)).followWith(bridge.getScrub)
    Await.result(f, 1 second)
    verify(fakeSender, never()).send(anyString, soapMessageWithArgument("Target", "0:00:06.000"))
  }

  @Test def isPlayingShouldSendGetTransportInfo() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("CurrentTransportState", "PLAYING"))
    }))
    Await.result(bridge.isPlaying, 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("GetTransportInfo"))
  }

  @Test def isPlayingShouldSendDecodeTransportStateWhenPlaying() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("CurrentTransportState", "PLAYING"))
    }))
    val playing = Await.result(bridge.isPlaying, 1 second)
    assertThat(playing).isTrue()
  }

  @Test def isPlayingShouldSendDecodeTransportStateWhenNotPlaying() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("CurrentTransportState", "STOPPED"))
    }))
    val playing = Await.result(bridge.isPlaying, 1 second)
    assertThat(playing).isFalse()
  }

  @Test def playShouldSendSetAVTransportURI() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.play("http://foo.com/bar.mov", 0.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("SetAVTransportURI"))
  }

  @Test def playShouldSendUriAsArgument() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.play("http://foo.com/bar.mov", 0.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithArgument("CurrentURI", "http://foo.com/bar.mov"))
  }

  @Test def setRateShouldSendPlayWhenRateIs1() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.setRate(1.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("Play"))
  }

  @Test def setRateShouldSetSpeedTo1WhenRateIs1() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.setRate(1.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithArgument("Speed", "1"))
  }

  @Test def setRateShouldSendPauseWhenRateIs0() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.setRate(0.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("Pause"))
  }

  @Test def setScrubShouldSendSeek() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.setScrub(10.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("Seek"))
  }

  @Test def setScrubShouldSendPositionAsArgumentInHMSFormat() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.setScrub(3666.5), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithArgument("Target", "1:01:06.500"))
  }

  @Test def setScrubShouldSendPositionAsRelTimeUnit() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    Await.result(bridge.setScrub(10.0), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithArgument("Unit", "REL_TIME"))
  }

  @Test def showPhotoShouldPublishAPhoto() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    when(publisher.publishPhoto(anyString, mockAny(classOf[InputStreamFactory]), anyInt)).thenReturn(Some("http://ms.com/resource"))
    Await.result(bridge.showPhoto(null, 1234, "transition"), 1 second)
    verify(publisher).publishPhoto(anyString, mockAny(classOf[InputStreamFactory]), mockEq(1234))
  }

  @Test def showPhotoShouldSendSetAVTransportURI() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    when(publisher.publishPhoto(anyString, mockAny(classOf[InputStreamFactory]), anyInt)).thenReturn(Some("http://ms.com/resource"))
    Await.result(bridge.showPhoto(null, 1234, "transition"), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("SetAVTransportURI"))
  }

  @Test def showPhotoShouldSendResourceUriAsArgumentToSetAVTransportURI() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    when(publisher.publishPhoto(anyString, mockAny(classOf[InputStreamFactory]), anyInt)).thenReturn(Some("http://ms.com/resource"))
    Await.result(bridge.showPhoto(null, 1234, "transition"), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithArgument("CurrentURI", "http://ms.com/resource"))
  }

  @Test def showPhotoShouldAlsoSendPlay() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    when(publisher.publishPhoto(anyString, mockAny(classOf[InputStreamFactory]), anyInt)).thenReturn(Some("http://ms.com/resource"))
    Await.result(bridge.showPhoto(null, 1234, "transition"), 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("Play"))
  }

  @Test(expectedExceptions = Array(classOf[IOException])) def showPhotoShouldThrowIfPublishingFails() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withNoArgReply())
    when(publisher.publishPhoto(anyString, mockAny(classOf[InputStreamFactory]), anyInt)).thenReturn(None)
    Await.result(bridge.showPhoto(null, 1234, "transition"), 1 second)
  }

  private def withReply(replyCreator: SoapMessage => SoapMessage): Answer[Object] = new Answer[Object] {
    def answer(invocation: InvocationOnMock) = {
      val msg = invocation.getArguments()(1).asInstanceOf[SoapMessage]
      future { replyCreator(msg) }
    }
  }

  private def createReply(msg: SoapMessage, args: (String, String)*) = {
    val reply = new SoapMessage(msg.getServiceType, msg.getName + "Reply")
    for (arg <- args) {
      reply.setArgument(arg._1, arg._2)
    }
    reply
  }

  private def withNoArgReply(): Answer[Object] = new Answer[Object] {
    def answer(invocation: InvocationOnMock) = {
      val msg = invocation.getArguments()(1).asInstanceOf[SoapMessage]
      future { createReply(msg) }
    }
  }
  private def soapMessageWithName(name: String): SoapMessage = argThat(new MessageNameMatcher(name))
  private def soapMessageWithArgument(name: String, value: String): SoapMessage = argThat(new ArgumentMatcher(name, value))
}

private class MessageNameMatcher(expectedName: String) extends BaseMatcher[SoapMessage] {
  override def matches(arg0: Object) = arg0.asInstanceOf[SoapMessage].getName == expectedName
  override def describeTo(arg0: Description) = arg0.appendText("SOAP message with name '" + expectedName + "'.")
}

private class ArgumentMatcher(name: String, expectedValue: String) extends BaseMatcher[SoapMessage] {
  override def matches(arg0: Object) = {
    val msg = arg0.asInstanceOf[SoapMessage]
    msg.getArgument(name, "--UNKNOWN--") == expectedValue
  }
  override def describeTo(arg0: Description) = arg0.appendText("SOAP message with argument named '" +
    name + "' with value '" + expectedValue + "'.")
}