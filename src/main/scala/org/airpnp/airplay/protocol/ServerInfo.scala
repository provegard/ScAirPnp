package org.airpnp.airplay.protocol

import org.airpnp.plist.KeyValue
import org.airpnp.plist.String
import org.airpnp.plist.Integer
import org.airpnp.plist.Dict
import org.airpnp.plist.PropertyList

class ServerInfo(private val deviceId: java.lang.String, private val features: Int, private val model: java.lang.String) {
  private val plist = {
    val pairs = Seq(
      new KeyValue("deviceid", new String(deviceId)),
      new KeyValue("features", new Integer(features)),
      new KeyValue("model", new String(model)),
      new KeyValue("protovers", new String("1.0")),
      new KeyValue("srcvers", new String("101.10")))

    new PropertyList(new Dict(pairs: _*))
  }

  def get() = plist
}