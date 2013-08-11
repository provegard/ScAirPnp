package org.airpnp.upnp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import org.mockito.Mockito.{ mock, when, verify }
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import org.testng.annotations.BeforeMethod
import org.airpnp.dlna.DLNAPublisher
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import org.mockito.Matchers.{ isA, anyString, argThat }
import org.mockito.Matchers.{ eq => mockEq }
import org.mockito.stubbing.Answer
import org.mockito.invocation.InvocationOnMock
import org.hamcrest.Matcher
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.fest.assertions.Assertions.assertThat
import org.airpnp.airplay.DurationAndPosition

private abstract class FakeSender {
  def send(url: String, msg: SoapMessage): Future[SoapMessage]
}

class AirPlayBridgeTest {
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

    bridge = new AirPlayBridge(device, sender, publisher)
  }

  @Test def getScrubShouldSendGetPositionInfo() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("TrackDuration", "0:00:59"), ("RelTime", "0:00:00"))
    }))
    Await.result(bridge.getScrub, 1 second)
    verify(fakeSender).send(anyString, soapMessageWithName("GetPositionInfo"))
  }

  @Test def soapSenderShouldGetControlUrl() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("TrackDuration", "0:00:59"), ("RelTime", "0:00:00"))
    }))
    Await.result(bridge.getScrub, 1 second)
    verify(fakeSender).send(mockEq("http://base.com/MediaRenderer_AVTransport/control"), isA(classOf[SoapMessage]))
  }

  @Test def getScrubShouldDecodeDurationAndPosition() {
    when(fakeSender.send(anyString, isA(classOf[SoapMessage]))).thenAnswer(withReply({
      msg => createReply(msg, ("TrackDuration", "0:00:59"), ("RelTime", "0:00:10"))
    }))
    val dp = Await.result(bridge.getScrub, 1 second)
    assertThat(dp).isEqualTo(DurationAndPosition(59.0, 10.0))
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

  private def soapMessageWithName(name: String): SoapMessage = argThat(new MessageNameMatcher(name))
}

private class MessageNameMatcher(expectedName: String) extends BaseMatcher[SoapMessage] {
  override def matches(arg0: Object) = arg0.asInstanceOf[SoapMessage].getName == expectedName
  override def describeTo(arg0: Description) = arg0.appendText("SOAP message with name '" + expectedName + "'")
}