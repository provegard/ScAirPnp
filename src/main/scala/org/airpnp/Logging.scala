package org.airpnp

import org.slf4j.LoggerFactory
import org.slf4j.Logger

trait Logging {
    val logger: Logger = LoggerFactory.getLogger(getClass)
    
    def debug(msg: String, os: AnyRef*) = logger.debug(msg, os)
    def trace(msg: String, os: AnyRef*) = logger.trace(msg, os)
    def info(msg: String, os: AnyRef*) = logger.info(msg, os)
    def warn(msg: String, os: AnyRef*) = logger.warn(msg, os)
    def error(msg: String, os: AnyRef*) = logger.error(msg, os)
    def error(msg: String, t: Throwable) = logger.error(msg, t)
}