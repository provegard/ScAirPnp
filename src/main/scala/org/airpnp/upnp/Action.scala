package org.airpnp.upnp

import scala.xml.Node

class Argument(protected val start: Node) extends XmlEntity {
  def getName() = text(_ \ "name").get
  def getDirection() = text(_ \ "direction").get
}

class Action(protected val start: Node, private val service: Service) extends XmlEntity {

  private val args = (start \\ "argumentList" \ "argument").map(new Argument(_))
  private val inargs = args.filter(_.getDirection == "in").map(_.getName).toList
  private val outargs = args.filter(_.getDirection == "out").map(_.getName).toList

  def getName() = text(_ \ "name").get
  def inputArguments() = inargs
  def outputArguments() = outargs

  def createSoapMessage(args: (String, Any)*): SoapMessage = {
    val msg = new SoapMessage(service.getServiceType, getName)
    val argMap = Map[String, Any](args: _*)
    inputArguments.filterNot(argMap.contains(_)).headOption match {
      case Some(x) => throw new IllegalArgumentException("Missing IN argument: " + x)
      case None =>
    }
    
    argMap.foreach(e => msg.setArgument(e._1, e._2.toString))
    msg
  }
}