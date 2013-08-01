package org.airpnp.upnp

import java.io.InputStream
import scala.xml.XML

object SoapError {
  def parse(is: InputStream) = {
    //TODO: error handling
    val err = XML.load(is) \\ "UPnPError"
    val errorCode = Integer.parseInt((err \ "errorCode").text)
    val errorDesc = (err \ "errorDescription").text
    new SoapError(errorCode, errorDesc)
  }
}

class SoapError private(val code: Int, val description: String) {
}