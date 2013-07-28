package org.airpnp.upnp

import scala.xml.Node

class Argument(protected val start: Node) extends XmlEntity {
  def getName() = text(_ \ "name").get
  def getDirection() = text(_ \ "direction").get
}

class Action(protected val start: Node) extends XmlEntity {

  private val args = (start \\ "argumentList" \ "argument").map(new Argument(_))

  def getName() = text(_ \ "name").get
  def inputArguments() = args.filter(_.getDirection == "in").map(_.getName)
  def outputArguments() = args.filter(_.getDirection == "out").map(_.getName)
}