package org.airpnp

import org.slf4j.LoggerFactory
import org.slf4j.Logger

trait Logging {
    protected val logger: Logger = LoggerFactory.getLogger(getClass)
    
    protected def debug(msg: String, os: AnyRef*) = logger.debug(msg, os: _*)
    protected def trace(msg: String, os: AnyRef*) = logger.trace(msg, os: _*)
    protected def info(msg: String, os: AnyRef*) = logger.info(msg, os: _*)
    protected def warn(msg: String, os: AnyRef*) = logger.warn(msg, os: _*)
    protected def error(msg: String, os: AnyRef*) = logger.error(msg, os: _*)
    protected def error(msg: String, t: Throwable) = logger.error(msg, t)
}