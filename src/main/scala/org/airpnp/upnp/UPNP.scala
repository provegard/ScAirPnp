package org.airpnp.upnp;

import org.airpnp.plumbing.Dynamic._
import java.io.IOException
import java.net.InetAddress
import java.net.MulticastSocket
import net.pms.network.UPNPHelper
import java.util.regex.Pattern
import java.util.Locale

object UPNP {
  val IPV4_UPNP_HOST = "239.255.255.250"
  val UPNP_PORT = 1900

  private val durationPattern = Pattern.compile("^[+-]?(?<hour>\\d+):(?<minute>\\d{2,2}):(?<second>\\d{2,2})(.(?<msec>\\d+)|.(?<F0>\\d+)/(?<F1>\\d+))?$")

  //    public static MulticastSocket getNewMulticastSocket() {
  //        MulticastSocket multicastSocket = (MulticastSocket) IgnoreMemberAccessibility
  //                .invokeStatic(UPNPHelper.class, "getNewMulticastSocket");
  //        return multicastSocket;
  //    }
  //
  def getUPNPAddress() = classOf[UPNPHelper].callStatic("getUPNPAddress").asInstanceOf[InetAddress]
  //
  //    public static void cleanupMulticastSocket(MulticastSocket socket) {
  //        // Clean up the multicast socket nicely
  //        try {
  //            socket.leaveGroup(getUPNPAddress());
  //        } catch (IOException e) {
  //        }
  //
  //        socket.disconnect();
  //        socket.close();
  //    }

  /**
   * Parses a string in duration format and returns as number of seconds. Format:
   *
   *  ['+'|'-']H+:MM:SS[.F0+|.F0/F1]
   */
  def parseDuration(duration: String): Double = {
    val m = durationPattern.matcher(duration)
    if (!m.matches) {
      throw new IllegalArgumentException("Invalid duration format.")
    }
    val hour = Integer.parseInt(m.group("hour"))
    val minute = Integer.parseInt(m.group("minute"))
    val second = Integer.parseInt(m.group("second"))

    if (minute >= 60 || second >= 60) {
      throw new IllegalArgumentException("Invalid duration format (minute/second too large).")
    }

    val msec = {
      val g = m.group("msec")
      if (g != null) {
        java.lang.Double.parseDouble("0." + g)
      } else {
        val f1g = m.group("F1")
        if (f1g != null) {
          val f1 = java.lang.Double.parseDouble(f1g)
          if (f1 == 0) {
            throw new IllegalArgumentException("Invalid duration format (millisecond divisor is 0).")
          }
          val f0 = java.lang.Double.parseDouble(m.group("F0"))
          if (f0 >= f1) {
            throw new IllegalArgumentException("Invalid duration format (milliseconds greater than 1).")
          }
          f0 / f1
        } else {
          0.0
        }
      }
    }

    val sign = if (duration(0) == '-') -1 else 1
    sign * (((hour * 60 + minute) * 60) + second + msec)
  }
  
  def toDuration(secs: Double): String = {
    def toHms(secs: Double) = {
	    val hour = (secs / 3600).toInt
	    val min = ((secs.toInt % 3600) / 60).toInt
	    val sec = (secs.toInt % 60) + secs - secs.toInt
	    "%d:%02d:%06.3f".formatLocal(Locale.US, hour, min, sec)
    }
    val hms = toHms(Math.abs(secs))
    if (secs < 0) "-" + hms else hms
  }
}
