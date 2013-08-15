package org.airpnp.dlna

import java.io.InputStream

trait DLNAPublisher {
  def publishPhoto(id: String, data: () => InputStream, len: Int): Option[String]
  def publishMovie(id: String, url: String): Option[String]
  def unpublish(id: String)
}

