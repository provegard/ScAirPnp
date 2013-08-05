package org.airpnp

import java.net.InetSocketAddress
import net.pms.PMS
import java.net.InetAddress
import net.pms.network.NetworkConfiguration
import net.pms.configuration.PmsConfiguration
import scala.xml.Node
import java.net.URLConnection
import java.net.HttpURLConnection
import java.net.URL
import scala.xml.XML

object Networking {

  def createDownloader(): String => Node = {
    loc =>
      {
        val url = new URL(loc)
        val is = url.openStream
        try {
          XML.load(is)
        } finally {
          is.close
        }
      }
  }

  def getInetAddress(): InetAddress = {
    val configuration = PMS.getConfiguration()
    val hostname = configuration.getServerHostname
    if (isEmpty(hostname)) {
      val ni = configuration.getNetworkInterface
      getAddressFromNetworkInterface(ni)
    } else {
      // Note: PMS's HTTPServer wants to do a network interface check also, but does it at a time
      // the network interface hasn't been set yet, so it probably doesn't work and so we don't
      // bother with doing something similar here.
      InetAddress.getByName(hostname)
    }
  }

  private def getAddressFromNetworkInterface(networkInterfaceName: String): InetAddress = {
    var ia = isEmpty(networkInterfaceName) match {
      case true => NetworkConfiguration.getInstance.getDefaultNetworkInterfaceAddress()
      case false => NetworkConfiguration.getInstance.getAddressForNetworkInterfaceName(networkInterfaceName)
    }

    if (ia != null) ia.getAddr else InetAddress.getLocalHost
  }

  private def isEmpty(s: String): Boolean = s == null || s.length == 0
}