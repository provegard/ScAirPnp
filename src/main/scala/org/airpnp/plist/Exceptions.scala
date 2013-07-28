package org.airpnp.plist;

import java.io.IOException

class PropertyListException(private val reason: java.lang.String) extends IOException(reason) {
}

/**
 * Represent a binary property list error due to an unhandled feature.
 */
class PropertyListUnhandledException(private val reason: java.lang.String) extends PropertyListException(reason) {
}

/**
 * Represents a binary property list format error.
 */
class PropertyListFormatException(private val reason: java.lang.String) extends PropertyListException(reason) {
}