package org.airpnp.dlna

import net.pms.dlna.DLNAResource
import java.io.InputStream
import net.pms.formats.FormatFactory
import net.pms.dlna.DLNAMediaInfo
import org.airpnp.Logging

class StreamablePhoto(id: String, streamFactory: () => InputStream,
  val length: Long) extends DLNAResource with Logging {
  private val name = id + ".jpg" // Assume JPEG for all iDevice photos...
  setFormat(FormatFactory.getAssociatedFormat(name))
  setMedia({
    val media = new DLNAMediaInfo()
    //TODO: set width, height and BPP also?
    media.setCodecV("jpg")
    media.setContainer("jpg")
    media
  })

  override def isFolder() = false
  override def getInputStream() = {
    trace("Input stream for streamable photo requested.")
    streamFactory()
  }

  def isValid() = true
  def getName() = name
  def getSystemName() = getName
}