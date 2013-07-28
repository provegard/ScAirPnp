package org.airpnp.upnp

import scala.xml.Node
import scala.xml.NodeSeq

trait XmlEntity {
  protected def start(): NodeSeq
  protected def text(x: Node => NodeSeq): Option[String] = start.headOption match {
    case Some(root: Node) => x.apply(root).headOption match { case Some(x: Node) => Some(x.text) case None => None }
    case None => None
  }
}