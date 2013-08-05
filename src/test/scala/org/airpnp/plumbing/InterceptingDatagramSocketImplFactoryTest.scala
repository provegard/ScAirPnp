package org.airpnp.plumbing

import org.fest.assertions.Assertions.assertThat
import org.airpnp.plumbing.Dynamic._
import org.testng.annotations.Test
import org.testng.annotations.BeforeClass
import java.net.DatagramSocket
import java.net.MulticastSocket

class InterceptingDatagramSocketImplFactoryTest {
  @BeforeClass def installFactory() {
    InterceptingDatagramSocketImplFactory.install(p => {})
  }
  
  @Test def shouldCreateCorrectImplObject() {
    val socket = new DatagramSocket()
    val impl = socket.call("getImpl")
    assertThat(impl).isInstanceOf(classOf[InterceptingDatagramSocketImpl])
  }
  
  @Test def shouldDetectUnicastDatagramSocket() {
    val socket = new DatagramSocket()
    val impl = socket.call("getImpl").asInstanceOf[InterceptingDatagramSocketImpl]
    assertThat(impl.isMulticast).isFalse()
  }

  @Test def shouldDetectMulticastDatagramSocket() {
    val socket = new MulticastSocket()
    val impl = socket.call("getImpl").asInstanceOf[InterceptingDatagramSocketImpl]
    assertThat(impl.isMulticast).isTrue()
  }
}