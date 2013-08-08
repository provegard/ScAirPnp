package org.airpnp.dlna

import java.util.concurrent.ConcurrentLinkedQueue
import org.airpnp.Logging
import net.pms.dlna.DLNAResource
import net.pms.dlna.virtual.VirtualFolder
import net.pms.PMS

class AirPnpFolder extends VirtualFolder("AirPnp", null) with Logging {

  def addDynamicChild(child: DLNAResource) {
    addChild(child)
  }

}