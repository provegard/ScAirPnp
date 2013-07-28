package org.airpnp.upnp

import javax.xml.soap.MessageFactory
import javax.xml.transform.TransformerFactory
import java.io.StringWriter
import javax.xml.transform.stream.StreamResult

object SoapMessage {
  private val messageFactory = MessageFactory.newInstance
  private val xform = TransformerFactory.newInstance.newTransformer
}

class SoapMessage(private val serviceType: String, private val name: String) {

  private val (soapPart, bodyElement) = {
    val soapMessage = SoapMessage.messageFactory.createMessage
    val soapPart = soapMessage.getSOAPPart
    val soapEnvelope = soapPart.getEnvelope

    // Header is optional, remove it
    val soapHeader = soapEnvelope.getHeader
    soapEnvelope.removeChild(soapHeader)

    val soapBody = soapEnvelope.getBody();
    val bodyName = soapEnvelope.createName(name, "u", serviceType);
    val bodyElement = soapBody.addBodyElement(bodyName);
    (soapPart, bodyElement)
  }

  def getName(): String = name
  def getHeader(): String = serviceType + "#" + getName()

  def getArgument(name: String, defaultValue: String): String = {
    val elems = bodyElement.getElementsByTagName(name)
    if (elems.getLength == 0) defaultValue else elems.item(0).getTextContent
  }

  def setArgument(name: String, value: String) = {
    val elems = bodyElement.getElementsByTagName(name)
    elems.getLength match {
      case 0 => bodyElement.addChildElement(name).setTextContent(value)
      case _ => elems.item(0).setTextContent(value)
    }
  }

  def deleteArgument(name: String) = {
    val elems = bodyElement.getElementsByTagName(name)
    elems.getLength match {
      case 0 => throw new IllegalArgumentException("No such argument: " + name)
      case _ => bodyElement.removeChild(elems.item(0))
    }
  }

  override def toString = {
    val source = soapPart.getContent

    val writer = new StringWriter
    val result = new StreamResult(writer)
    SoapMessage.xform.transform(source, result)
    writer.toString
  }
}