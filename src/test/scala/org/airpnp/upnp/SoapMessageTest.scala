package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import javax.xml.soap.MessageFactory

class SoapMessageTest {
  private var message: SoapMessage = null

  @BeforeMethod
  def createMessage() {
    message = new SoapMessage("type", "action")
  }

  @Test
  def shouldHaveAName() {
    assertThat(message.getName()).isEqualTo("action")
  }

  @Test def shouldHaveAServiceType() {
    assertThat(message.getServiceType).isEqualTo("type")
  }

  @Test
  def shouldBeAbleToConstructAHeader() {
    assertThat(message.getSoapAction).isEqualTo("\"type#action\"")
  }

  @Test
  def shouldReturnDefaultValueForMissingArgument() {
    assertThat(message.getArgument("foo", "dflt")).isEqualTo("dflt")
  }

  @Test
  def shouldBeAbleToSetArgument() {
    message.setArgument("foo", "bar")
    assertThat(message.getArgument("foo", "dflt")).isEqualTo("bar")
  }

  @Test
  def shouldBeAbleToReturnFunctionLikeString() {
    message.setArgument("foo", "bar")
    message.setArgument("bar", "baz")
    assertThat(message.toFunctionLikeString).isEqualTo("\"type#action\"(foo: bar, bar: baz)")
  }

  @Test
  def shouldBeAbleToModifyAnArgument() {
    message.setArgument("foo", "bar")
    message.setArgument("foo", "baz")
    assertThat(message.getArgument("foo", "dflt")).isEqualTo("baz")
  }
  @Test
  def shouldBeAbleToDeleteArgument() {
    message.setArgument("foo", "bar")
    message.deleteArgument("foo")
    assertThat(message.getArgument("foo", "dflt")).isEqualTo("dflt")
  }

  @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
  def shouldThrowIfArgumentDoesntExistWhenTryingToDelete() {
    message.deleteArgument("foo")
  }

  @Test
  def shouldBeAbleToCreateStringRepresentation() {
    val rep = message.toString
    assertThat(rep).contains("<u:action")
  }
}