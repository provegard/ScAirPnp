package org.airpnp.airplay.protocol

import org.airpnp.plist.Array
import org.airpnp.plist.String
import org.airpnp.plist.Dict
import org.airpnp.plist.KeyValue
import org.airpnp.plist.PropertyList

class SlideshowFeatures {
  private val plist = {
    val innerDict = new Dict(new KeyValue("key", new String("UPnP")), new KeyValue("name", new String("UPnP")))
    val outerDict = new Dict(new KeyValue("themes", new Array(innerDict)))
    new PropertyList(outerDict)
  }

  def get() = plist
}