package org.airpnp.upnp

import org.airpnp.Util

class UPnPMessage(private val data: String) {

  private val (method, udn, _type, headers) = {
    val lines = data.split("\r\n")
    val method = lines(0).split(" ")(0)
    val headers = splitHeaders(lines.slice(1, lines.length))
    val udnAndType = extractUdnAndType(headers)
    (method, udnAndType._1, udnAndType._2, headers)
  }

  private def extractUdnAndType(headers: Map[String, String]): (Option[String], Option[String]) = {
    val usn = headers.getOrElse("USN", null)
    if (usn != null) {
      val parts = Util.splitUsn(usn)
      return (Some(parts._1), Some(parts._2))
    }
    (None, None)
  }

  private def splitHeaders(lines: Array[String]): Map[String, String] = {
    lines.takeWhile(_ != "").map(line => {
      val parts = line.split("\\s*:\\s*", 2)
      (parts(0).toUpperCase, parts(1))
    }).toMap
  }

  def getType() = _type
  def getUdn() = udn
  def getLocation() = headers.get("LOCATION")
  def isNotification() = method == "NOTIFY"
  def getNotificationSubType() = headers.get("NTS")
  def getHeaders() = headers
  def getMethod() = method
  def isAlive() = getNotificationSubType match { case Some(s) => s == "ssdp:alive" case None => false }
  def isByeBye() = getNotificationSubType match { case Some(s) => s == "ssdp:byebye" case None => false }
  override def toString() = data
}