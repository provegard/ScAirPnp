package org.airpnp

import org.testng.annotations.BeforeClass
import org.slf4j.LoggerFactory

trait TraceLogging {
  @BeforeClass def bumpLogLevel() {
    val logger = LoggerFactory.getLogger("org.airpnp").asInstanceOf[ch.qos.logback.classic.Logger]
    logger.setLevel(ch.qos.logback.classic.Level.TRACE)
  }
}