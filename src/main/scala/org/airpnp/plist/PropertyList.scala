package org.airpnp.plist

import scala.xml.dtd.DocType
import scala.xml.dtd.PublicID
import scala.xml.Utility

object PropertyList {
  val CT_BINARY_PLIST = "application/x-apple-binary-plist"
  val CT_TEXT_PLIST = "text/x-apple-plist+xml"
}

class PropertyList(val root: PropertyListObject[_]) {
  if (root == null) {
    throw new IllegalArgumentException("Root object cannot be null.")
  }

  def toXml() = {
    val gen = new XmlPropertyListGenerator
    val doc = gen.generate(this)
    val w = new java.io.StringWriter
    
    val doctype = DocType("plist", PublicID("-//Apple//DTD PLIST 1.0//EN", "http://www.apple.com/DTDs/PropertyList-1.0.dtd"), Nil)
    
    // Essentially what xml.XML.write does, but we control quotes and newlines!
    w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    w.write( doctype.toString())
    w.write(Utility.serialize(doc.docElem).toString)

    w.toString
  }
}
