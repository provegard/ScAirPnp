package org.airpnp.upnp

import scala.xml.Node
import scala.collection.mutable.MutableList
import org.airpnp.Util
import scala.concurrent.Future

object Device {
  private val AVTRANSPORT_SERVICE_TYPE = "urn:schemas-upnp-org:service:AVTransport:1"
  private val CONNMANAGER_SERVICE_TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1"
  private val requiredServiceTypes = Seq(AVTRANSPORT_SERVICE_TYPE, CONNMANAGER_SERVICE_TYPE)

  type SoapSender = (String, SoapMessage) => Future[SoapMessage]
}

class Device(private val root: Node, private val baseUrl: String) extends XmlEntity {

  // Mandatory XML elements for a device: deviceType, friendlyName, manufacturer, modelName, UDN
  protected val start = root \ "device"
  private val services = Map((start \ "serviceList" \ "service").map(new Service(_, baseUrl)).map({ s => s.getServiceId -> s }): _*)

  private var _soapSender: Device.SoapSender = null

  def soapSender = _soapSender
  def soapSender_=(s: Device.SoapSender) {
    if (s != null && _soapSender != null) {
      throw new IllegalStateException("SOAP sender already set for " + getFriendlyName)
    }
    _soapSender = s
  }

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