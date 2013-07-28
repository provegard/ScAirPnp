package org.airpnp.upnp

import scala.xml.Node
import scala.xml.NodeSeq

class Device(private val root: Node, private val baseUrl: String) extends XmlEntity {
    // Mandatory XML elements for a device: deviceType, friendlyName, manufacturer, modelName, UDN
  protected val start = root \ "device"
  private val services = Map((start \ "serviceList" \ "service").map(new Service(_, baseUrl)).map({s => s.getServiceId -> s}): _*)

//    private Map<String, Service> readServices() {
//        Map<String, Service> result = new HashMap<String, Service>();
//        for (Node n : Util.findNodes(root, serviceExpression)) {
//            Service s = new Service(n, getBaseUrl());
//            result.put(s.attr("serviceId"), s);
//        }
//        return result;
//    }
  
  def getBaseUrl() = baseUrl

    override def toString() = "%s [UDN=%s]".format(getFriendlyName, getUdn);

  def getServices() = services.map(_._2)
  def getServiceById(id: String): Option[Service] = services.get(id)
  
//    public Collection<Service> getServices() {
//        return unmodifiableCollection(services.values());
//    }
//
//    public Service getServiceById(String serviceId) {
//        return services.get(serviceId);
//    }
//    
//    //TODO: Test!
//    public String getUdn() {
//        return attr("UDN");
//    }
//    
//    //TODO: Test!
//    public String getFriendlyName() {
//        return attr("friendlyName");
//    }
    def getUdn() = text(_ \ "UDN").get
    def getFriendlyName() = text(_ \ "friendlyName").get
}