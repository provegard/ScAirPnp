package org.airpnp.plist

import org.fest.assertions.Assertions.assertThat

import java.util.ArrayList
import java.util.List

import org.testng.annotations.Test

class PropertyListToXmlTest {

  @Test
  def shouldGenerateProperXml() {

    val pairs = Seq(
      new KeyValue("deviceid", new String("abc")),
      new KeyValue("features", new Integer(123)),
      new KeyValue("model", new String("AppleTV")),
      new KeyValue("protovers", new String("1.0")),
      new KeyValue("test", True.INSTANCE))

    val plist = new PropertyList(new Dict(pairs: _*))
    val xml = plist.toXml

    assertThat(xml)
      .isEqualTo(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
          + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">"
          + "<plist version=\"1.0\">" + "<dict>"
          + "<key>deviceid</key>"
          + "<string>abc</string>"
          + "<key>features</key>"
          + "<integer>123</integer>"
          + "<key>model</key>"
          + "<string>AppleTV</string>"
          + "<key>protovers</key>"
          + "<string>1.0</string>"
          + "<key>test</key>"
          + "<true/>" + "</dict>"
          + "</plist>")
  }
}
