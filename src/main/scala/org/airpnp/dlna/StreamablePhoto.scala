package org.airpnp.dlna

import net.pms.dlna.DLNAResource
import java.io.InputStream
import net.pms.formats.FormatFactory
import net.pms.dlna.DLNAMediaInfo
import org.airpnp.Logging

class StreamablePhoto(streamFactory: => InputStream,
  val length: Long) extends DLNAResource with Logging {

  setFormat(FormatFactory.getAssociatedFormat("dummy.jpg"))
  setMedia(new DLNAMediaInfo())

  override def isFolder() = false
  override def getInputStream() = {
    trace("Input stream for streamable photo requested.")
    streamFactory
  }
  
  def isValid() = true
  def getName() = "dummy.jpg"
  def getSystemName() = getName
}