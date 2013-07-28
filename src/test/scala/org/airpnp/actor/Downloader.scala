package org.airpnp.actor

import scala.xml.Node
import java.util.regex.Pattern
import scala.xml.XML

object Downloader {
  private val pat = Pattern.compile("^http://(.*)\\.com(/.*)?$")
  def create(): String => Node = {
    return s => {
      var m = pat.matcher(s)
      if (!m.matches) {
        throw new IllegalArgumentException("Unknown URL: " + s)
      }
      val folder = m.group(1)
      var path = m.group(2)
      if (path == null || path == "/" || path == "") {
        path = "/root.xml"
      }
      val is = getClass.getResourceAsStream("/org/airpnp/upnp/" + folder + path)
      XML.load(is)
    }
  }
}