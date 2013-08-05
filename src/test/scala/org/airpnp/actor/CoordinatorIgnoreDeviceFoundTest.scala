package org.airpnp.actor

import org.fest.assertions.Assertions.assertThat
import org.airpnp.actor.ActorTestUtil._
import scala.actors.Actor._
import org.testng.annotations.Test
import scala.collection.mutable.MutableList
import scala.actors.Actor
import org.testng.Assert.fail
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.testng.annotations.BeforeClass

class CoordinatorIgnoreDeviceFoundTest {
  private var result = false
  private var deviceBuilder: TestDeviceBuilder = null

  @BeforeClass def setup() {
    val latch = new CountDownLatch(1)
    val opts = new CoordinatorOptions()
    deviceBuilder = new TestDeviceBuilder(latch)
    opts.deviceBuilder = deviceBuilder

    val coord = new Coordinator(opts)
    coord.start()

    stopper(coord) {
      coord ! DeviceShouldBeIgnored("uuid:abcd", "because I say so")
      coord ! DeviceFound("uuid:abcd", "http://foo.com")
      result = latch.await(500, TimeUnit.MILLISECONDS)
    }
  }

  @Test def shouldNotSendBuildToBuilderInResponseToDeviceFound() {
    assertThat(result).isFalse
  }
}