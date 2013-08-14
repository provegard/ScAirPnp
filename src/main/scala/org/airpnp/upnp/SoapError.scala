package org.airpnp.upnp

import java.io.InputStream
import scala.xml.XML
import java.io.StringWriter
import scala.xml.PrettyPrinter

object SoapError {
  def parse(is: InputStream) = {
    //TODO: error handling
    val root = XML.load(is)
    
    val pp = new PrettyPrinter(80, 2)
    val xml = pp.format(root)
    
    val err = root \\ "UPnPError"
    val errorCode = Integer.parseInt((err \ "errorCode").text)
    val errorDesc = (err \ "errorDescription").text
    new SoapError(errorCode, errorDesc, xml)
  }
}

class SoapError private(val code: Int, val description: String, val xml: String) extends RuntimeException(code + " " + description) {
}