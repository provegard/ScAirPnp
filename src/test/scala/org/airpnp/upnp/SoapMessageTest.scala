package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import javax.xml.soap.MessageFactory

class SoapMessageTest {
    private var message: SoapMessage = null

    @BeforeMethod
    def createMessage(): Unit = {
        message = new SoapMessage("type", "action")
    }
    
    @Test
    def shouldHaveAName(): Unit = {
        assertThat(message.getName()).isEqualTo("action")
    }

    @Test
    def shouldBeAbleToConstructAHeader(): Unit = {
        assertThat(message.getHeader()).isEqualTo("type#action")
    }
    
    @Test
    def shouldReturnDefaultValueForMissingArgument(): Unit = {
        assertThat(message.getArgument("foo", "dflt")).isEqualTo("dflt")
    }
    
    @Test
    def shouldBeAbleToSetArgument(): Unit = {
        message.setArgument("foo", "bar")
        assertThat(message.getArgument("foo", "dflt")).isEqualTo("bar")
    }
    
    @Test
    def shouldBeAbleToModifyAnArgument(): Unit = {
        message.setArgument("foo", "bar")
        message.setArgument("foo", "baz")
        assertThat(message.getArgument("foo", "dflt")).isEqualTo("baz")
    }
    @Test
    def shouldBeAbleToDeleteArgument(): Unit = {
        message.setArgument("foo", "bar")
        message.deleteArgument("foo")
        assertThat(message.getArgument("foo", "dflt")).isEqualTo("dflt")
    }
    
    @Test(expectedExceptions = Array(classOf[IllegalArgumentException]))
    def shouldThrowIfArgumentDoesntExistWhenTryingToDelete(): Unit = {
        message.deleteArgument("foo")
    }

    @Test
    def shouldBeAbleToCreateStringRepresentation(): Unit = {
        val rep = message.toString
        assertThat(rep).contains("<u:action")
    }
}