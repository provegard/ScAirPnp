package org.airpnp.upnp	

import scala.collection.JavaConversions._
import javax.xml.soap.MessageFactory
import javax.xml.transform.TransformerFactory
import java.io.StringWriter
import javax.xml.transform.stream.StreamResult
import java.io.InputStream
import scala.xml.XML
import scala.xml.Node
import javax.xml.soap.SOAPElement

object SoapMessage {
  private val messageFactory = MessageFactory.newInstance
  private val xform = TransformerFactory.newInstance.newTransformer

  def parse(is: InputStream) = new SoapMessage(is)
}

class SoapMessage private (private val serviceTypeIn: String, private val nameIn: String, private val is: InputStream) {
  private val (soapPart, bodyElement, serviceType: String, name: String) = {
    if (is != null) {
    	val soapMessage = SoapMessage.messageFactory.createMessage(null, is)
	    val soapPart = soapMessage.getSOAPPart
	    val soapEnvelope = soapPart.getEnvelope

	    val soapBody = soapEnvelope.getBody
	    val bodyElement = soapBody.getChildElements.toSeq.filter(x => x.isInstanceOf[SOAPElement]).head.asInstanceOf[SOAPElement]
    	(soapPart, bodyElement, bodyElement.getNamespaceURI(), bodyElement.getLocalName)
    } else {
	    val soapMessage = SoapMessage.messageFactory.createMessage
	    val soapPart = soapMessage.getSOAPPart
	    val soapEnvelope = soapPart.getEnvelope
	
	    // Header is optional, remove it
	    val soapHeader = soapEnvelope.getHeader
	    soapEnvelope.removeChild(soapHeader)
	
	    val soapBody = soapEnvelope.getBody();
	    val bodyName = soapEnvelope.createName(nameIn, "u", serviceTypeIn)
	    val bodyElement = soapBody.addBodyElement(bodyName)
	    (soapPart, bodyElement, serviceTypeIn, nameIn)
    }
  }

  def this(serviceType: String, name: String) = this(serviceType, name, null)
  def this(is: InputStream) = this(null, null, is)

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

  def isFault() = getName == "Fault"
}