package org.airpnp.actor

import scala.actors.Actor
import java.util.concurrent.CountDownLatch

object ActorTestUtil {
  def stopper(a: Actor)(action: => Unit) {
    try {
      action
    } finally {
      a !? Stop
    }
  }
}

private[actor] class TestDeviceBuilder(private val latch: CountDownLatch) extends Actor {
  var message: Build = null
  
  def act() {
    loop {
      react {
        case x: Build => {
          message = x
          latch.countDown()
        }
        case Stop => {
          sender ! Stopped
          exit
        }
      }
    }
  }
}

