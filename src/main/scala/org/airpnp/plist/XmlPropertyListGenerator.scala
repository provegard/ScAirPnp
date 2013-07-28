package org.airpnp.plist

import javax.xml.bind.DatatypeConverter
import scala.xml.Document
import scala.collection.mutable.ListBuffer
import scala.xml.Node
import scala.xml.Elem
import java.text.SimpleDateFormat
import scala.xml.TopScope
import scala.xml.Null
import scala.xml.UnprefixedAttribute
import scala.xml.Text
import scala.xml.NodeSeq

class XmlPropertyListGenerator extends PropertyListObjectVisitor {
  private val iso8601 = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
  private var nodes: ListBuffer[Node] = null

  def generate(plist: PropertyList): Document = {
    nodes = new ListBuffer[Node]
    plist.root.accept(this)
    val doc = new Document
    doc.docElem = new Elem(null, "plist", new UnprefixedAttribute("version", "1.0", Null), TopScope, true, nodes: _*)
    doc
  }

  def visit(true1: True) = {
    nodes += <true/>
  }

  def visit(real: Real) = {
    nodes += <real>{ real.getValue().toString() }</real>
  }

  def visit(string: String) = {
    nodes += <string>{ string.getValue() }</string>
  }

  def visit(array: Array) = {
    val oldNodes = nodes
    nodes = new ListBuffer[Node]

    for (plo <- array.getValue()) {
      plo.accept(this)
    }
    oldNodes += new Elem(null, "array", Null, TopScope, true, nodes: _*)

    nodes = oldNodes
  }

  def visit(date: Date) = {
    nodes += <date>{ iso8601.format(date.getValue()) }</date>
  }

  def visit(dict: Dict) = {
    val oldNodes = nodes
    nodes = new ListBuffer[Node]

    for (pair <- dict.entries) {
      nodes += <key>{ pair.getKey }</key>
      pair.getValue.accept(this)
    }
    oldNodes += new Elem(null, "dict", Null, TopScope, true, nodes: _*)
    nodes = oldNodes
  }

  def visit(false1: False) = {
    nodes += <false/>
  }

  def visit(integer: Integer) = {
    nodes += <integer>{ integer.getValue().toString() }</integer>
  }

  def visit(data: Data) = {
    val b64 = DatatypeConverter.printBase64Binary(data.getValue.toArray)
    nodes += <data>{ b64 }</data>
  }

}
