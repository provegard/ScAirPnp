package org.airpnp.upnp

import scala.xml.Node
import java.net.URL

class Service(protected val start: Node, private val baseUrl: String) extends XmlEntity {
  private var actions: Map[String, Action] = null
  private val baseUrlUrl = new URL(baseUrl)
  
  def getServiceId() = text(_ \ "serviceId").get
  def getServiceType() = text(_ \ "serviceType").get

  def initialize(scpdElement: Node) = {
    actions = Map((scpdElement \\ "actionList" \ "action").map(new Action(_)).map({ a => a.getName -> a }): _*)
  }

  def action(name: String): Option[Action] = actions.get(name)
  
  def getSCPDURL() = new URL(baseUrlUrl, text(_ \ "SCPDURL").get).toString
}