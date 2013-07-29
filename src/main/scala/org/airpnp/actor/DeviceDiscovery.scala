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

case object CheckLiveness
case object DoDiscovery
case class DeviceFound(val udn: String, val location: String)

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
          trace("Device discovery was stopped.")
          sender ! Stopping
          exit
        }
      }
    }
  }

  private def doDiscovery() = {
    sendMSearch("upnp:rootdevice", p => {
      val data = new String(p.getData(), 0, p.getLength())
      val msg = new UPnPMessage(data)
      msg.getLocation match {
        case Some(loc) => msg.getUdn match {
          case Some(udn) => sender ! DeviceFound(udn, loc)
          case None => // ignore
        }
        case None => // ignore
      }
    })
  }

  private def sendMSearch(st: String, handler: DatagramPacket => Unit) = {
    val s = new DatagramSocket
    try {
      // DatagramSocket s = new DatagramSocket();
      val upnpAddress = UPNP.getUPNPAddress

      val msg = new MSearchRequest(st, DeviceDiscovery.MX).toString.getBytes
      val ssdpPacket = new DatagramPacket(msg, msg.length,
        upnpAddress, UPNP.UPNP_PORT)
      s.send(ssdpPacket)

      val endTime = System.currentTimeMillis + 1000 * DeviceDiscovery.MX
      +DeviceDiscovery.EXTRA_WAIT_TIME
      val buf = new Array[Byte](1024)
      var delay = endTime - System.currentTimeMillis
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
  //
  //    @Override
  //    public void interceptReceive(DatagramPacket packet) {
  //        PacketEvent event = new PacketEvent(packet, "intercepted");
  //        if (event.getMessage().isNotification()) {
  //            eventCoordinator.publish(event);
  //        }
  //    }

}