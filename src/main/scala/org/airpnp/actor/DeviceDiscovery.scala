package org.airpnp.actor

import org.airpnp.Logging
import scala.actors.Actor
import scala.actors.Actor._
import java.net.DatagramPacket
import java.net.DatagramSocket
import org.airpnp.upnp.UPNP
import org.airpnp.upnp.MSearchRequest
import java.net.SocketTimeoutException
import org.airpnp.upnp.UPnPMessage

object DeviceDiscovery {
  private val MX = 3
  private val EXTRA_WAIT_TIME = 500l
}

class DeviceDiscovery extends Actor with Logging {

  def act() = {
    loop {
      react {
        case DoDiscovery => doDiscovery
        case Stop => {
          debug("Device discovery was stopped.")
          sender ! Stopped
          exit
        }
      }
    }
  }

  private def doDiscovery() = {
    sendMSearch("upnp:rootdevice", p => {
      val msg = new UPnPMessage(p)
      if (msg.isBuildable) {
        sender ! DeviceFound(msg.getUdn.get, msg.getLocation.get)
      }
    })
  }

  private def sendMSearch(st: String, handler: DatagramPacket => Unit) = {
    val s = new DatagramSocket
    try {
      val upnpAddress = UPNP.getUPNPAddress

      val msg = new MSearchRequest(st, DeviceDiscovery.MX).toString.getBytes
      val ssdpPacket = new DatagramPacket(msg, msg.length,
        upnpAddress, UPNP.UPNP_PORT)
      s.send(ssdpPacket)

      val endTime = System.currentTimeMillis + 1000 * DeviceDiscovery.MX
      +DeviceDiscovery.EXTRA_WAIT_TIME
      val buf = new Array[Byte](1024)
      var delay = endTime - System.currentTimeMillis
      //TODO: Rewrite with NIO/non-blocking
      while (delay > 0) {
        s.setSoTimeout(delay.asInstanceOf[Int]);
        val incoming = new DatagramPacket(buf, buf.length)
        try {
          s.receive(incoming)
          handler.apply(incoming)
        } catch {
          // Ok, time's up, the while condition exits the loop
          case _: SocketTimeoutException => ()
        }
        delay = endTime - System.currentTimeMillis
      }
    } finally {
      s.close
    }
  }
}