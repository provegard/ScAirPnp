package org.airpnp.plist

import org.fest.assertions.Assertions.assertThat

import java.io.StringWriter
import java.util.ArrayList
import java.util.GregorianCalendar
import java.util.List

import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

import org.testng.annotations.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node

class XmlPropertyListGeneratorTest {
  @Test
  def shouldCreateEnclosingPlistElement(): Unit = {
    val plist = new PropertyList(True.INSTANCE)
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(doc.docElem.label).isEqualTo("plist")
  }

  @Test
  def shouldAddVersionToEnclosingPlistElement(): Unit = {
    val plist = new PropertyList(True.INSTANCE)
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat((root \ "@version").toString).isEqualTo("1.0")
  }

  @Test
  def shouldSupportTruePrimitive(): Unit = {
    val plist = new PropertyList(True.INSTANCE)
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<true/>")
  }

  @Test
  def shouldSupportFalsePrimitive(): Unit = {
    val plist = new PropertyList(False.INSTANCE)
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<false/>")
  }

  @Test
  def shouldSupportIntegerPrimitive(): Unit = {
    val plist = new PropertyList(new Integer(-42))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<integer>-42</integer>")
  }

  @Test
  def shouldSupportStringPrimitive(): Unit = {
    val plist = new PropertyList(new String("foo"))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<string>foo</string>")
  }

  @Test
  def shouldSupportRealPrimitive(): Unit = {
    val plist = new PropertyList(new Real(-3.14))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<real>-3.14</real>")
  }

  @Test
  def shouldUseExponentForBigRealPrimitive(): Unit = {
    val plist = new PropertyList(new Real(1.1e100))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<real>1.1E100</real>")
  }

  @Test
  def shouldSupportDatePrimitive(): Unit = {
    val d = new GregorianCalendar(2013, 6, 22, 23, 56, 30).getTime()
    val plist = new PropertyList(new Date(d))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<date>20130722T235630Z</date>")
  }

  @Test
  def shouldSupportDataPrimitive(): Unit = {
    val plist = new PropertyList(new Data("foobar".getBytes()))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<data>Zm9vYmFy</data>")
  }

  @Test
  def shouldSupportArrayCollection(): Unit = {
    val plist = new PropertyList(new Array(True.INSTANCE, False.INSTANCE))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<array><true/><false/></array>")
  }

  @Test
  def shouldSupportDictCollection(): Unit = {
    val pairs = Seq(
      new KeyValue("z", True.INSTANCE),
      new KeyValue("a", new Integer(42)))

    val plist = new PropertyList(new Dict(pairs))
    val doc = new XmlPropertyListGenerator().generate(plist)

    val root = doc.docElem
    assertThat(root.child(0).toString).isEqualTo("<dict><key>z</key><true/><key>a</key><integer>42</integer></dict>")
  }

}
