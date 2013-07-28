package org.airpnp

import net.pms.external.ExternalListener
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import net.pms.PMS

class AirPnp extends ExternalListener with Logging {

  info("AirPnp plugin starting!");
  
  def config(): javax.swing.JComponent = null
  
  def name(): String = "AirPnp"
    
  def shutdown(): Unit = {
    info("AirPnp plugin shutting down!");
  }
}