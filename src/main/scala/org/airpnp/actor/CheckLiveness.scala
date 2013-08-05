package org.airpnp.actor

import scala.actors.Actor
import org.airpnp.Logging
import org.airpnp.upnp.Device

//class CheckLiveness extends Actor with Logging {
//
//  def act() {
//    loop {
//      react {
//        case DoLivenessCheck => {}
//        case Stop => {
//          debug("Liveness checker was stopped.")          
//          sender ! Stopped
//          exit
//        }
//      }
//    }
//  }
//}