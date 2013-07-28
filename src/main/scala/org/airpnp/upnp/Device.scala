package org.airpnp.upnp

import scala.xml.Node
import scala.collection.mutable.MutableList
import org.airpnp.Util

object Device {
  private val AVTRANSPORT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"
  private val CONNMANAGER_SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1"
  private val requiredServiceTypes = Seq(AVTRANSPORT_SERVICE_TYPE, CONNMANAGER_SERVICE_TYPE)
}

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

  def isMediaRenderer() = {
    val actualServiceTypes = getServices.map(_.getServiceType)
    Device.requiredServiceTypes.forall(s => actualServiceTypes.exists(Util.areServiceTypesCompatible(s, _)))
  }
}