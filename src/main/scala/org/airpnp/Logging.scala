package org.airpnp

import org.slf4j.LoggerFactory
import org.slf4j.Logger

trait Logging {
    protected val logger: Logger = LoggerFactory.getLogger(getClass)
    
    protected def debug(msg: String, os: AnyRef*) = logger.debug(msg, os)
    protected def trace(msg: String, os: AnyRef*) = logger.trace(msg, os)
    protected def info(msg: String, os: AnyRef*) = logger.info(msg, os)
    protected def warn(msg: String, os: AnyRef*) = logger.warn(msg, os)
    protected def error(msg: String, os: AnyRef*) = logger.error(msg, os)
    protected def error(msg: String, t: Throwable) = logger.error(msg, t)
}