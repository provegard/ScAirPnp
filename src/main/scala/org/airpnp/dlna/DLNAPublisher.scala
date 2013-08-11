package org.airpnp.dlna

import java.io.InputStream

trait DLNAPublisher {
  def publishPhoto(id: String, data: => InputStream, len: Int): String  
  def publishMovie(id: String, url: String): String
  def unpublish(id: String)
}

