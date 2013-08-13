package org.airpnp.actor

import scala.actors.Actor
import org.airpnp.Logging

abstract class BaseActor extends Actor with Logging {
  override def react(f: PartialFunction[Any, Unit]): Nothing = {
    super.react {
      case x =>
        trace("'{}' received message {} from '{}'.", toString, x.toString, sender.toString)
        f(x)
    }
  }
}