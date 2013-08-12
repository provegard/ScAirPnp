package org.airpnp

import org.slf4j.LoggerFactory
import org.slf4j.Logger

trait Logging {
    protected val logger: Logger = LoggerFactory.getLogger(getClass)
    
    protected def debug(msg: String, os: Any*) = logger.debug(msg, os.map(_.toString): _*)
    protected def trace(msg: String, os: Any*) = logger.trace(msg, os.map(_.toString): _*)
    protected def info(msg: String, os: Any*) = logger.info(msg, os.map(_.toString): _*)
    protected def warn(msg: String, os: Any*) = logger.warn(msg, os.map(_.toString): _*)
    protected def error(msg: String, os: Any*) = logger.error(msg, os.map(_.toString): _*)
    protected def error(msg: String, t: Throwable) = logger.error(msg, t)
}