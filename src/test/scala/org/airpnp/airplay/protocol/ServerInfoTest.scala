package org.airpnp.airplay.protocol

import scala.collection.JavaConversions._
import java.util.ArrayList
import java.util.Iterator
import java.util.List
import org.testng.annotations.BeforeClass
import org.testng.annotations.DataProvider
import org.airpnp.plist.PropertyList

class ServerInfoTest extends PropertyListTester {

  var plist: PropertyList = null

  @BeforeClass
  def setup() = {
    plist = new ServerInfo("did", 100, "FooTV").get
  }

  @DataProvider
  def dictOrder(): java.util.Iterator[Array[Object]] = {
    Seq(
      Array[Object]("/", Array("deviceid", "features", "model", "protovers", "srcvers"))).iterator
  }

  @DataProvider
  def expectedData(): java.util.Iterator[Array[Object]] = {
    Seq(
      Array[Object]("/deviceid", "did"),
      Array[Object]("/features", 100l: java.lang.Long),
      Array[Object]("/model", "FooTV")).iterator
  }

}
