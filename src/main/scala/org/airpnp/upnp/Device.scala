package org.airpnp.upnp

import scala.xml.Node
import scala.xml.NodeSeq

class Device(private val root: Node, private val baseUrl: String) extends XmlEntity {
  // Mandatory XML elements for a device: deviceType, friendlyName, manufacturer, modelName, UDN
  protected val start = root \ "device"
  private val services = Map((start \ "serviceList" \ "service").map(new Service(_, baseUrl)).map({ s => s.getServiceId -> s }): _*)

  def getBaseUrl() = baseUrl

  override def toString() = "%s [UDN=%s]".format(getFriendlyName, getUdn);

  def getServices() = services.map(_._2)
  def getServiceById(id: String): Option[Service] = services.get(id)

  def getUdn() = text(_ \ "UDN").get
  def getFriendlyName() = text(_ \ "friendlyName").get
}