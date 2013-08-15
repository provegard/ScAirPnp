package org.airpnp.dlna

import scala.collection.JavaConversions._
import java.util.concurrent.ConcurrentLinkedQueue
import org.airpnp.Logging
import net.pms.dlna.DLNAResource
import net.pms.dlna.virtual.VirtualFolder
import net.pms.PMS
import scala.collection.mutable.HashMap
import net.pms.dlna.virtual.TranscodeVirtualFolder
import net.pms.dlna.WebVideoStream
import java.io.InputStream

class AirPnpFolder(baseUrl: => String) extends VirtualFolder("AirPnp", null) with DLNAPublisher with Logging {

  private val published = new HashMap[String, DLNAResource]()

  private def addDynamicResource(child: DLNAResource): Boolean = {
    val sizeBefore = getChildren().size
    addChild(child)
    if (sizeBefore == getChildren().size) {
      error("Failed to add DLNA resource: {}", child)
      return false
    }
    notifyRefresh()
    true
  }

  private def removeDynamicResource(child: DLNAResource) {
    // Remove the child
    // Remove any children where c.first == child
    // getTranscodeFolder. If it exists, remove any children where c.getName == child.getName
    val children = getChildren
    val it = children.listIterator()
    while (it.hasNext) {
      val c = it.next()
      if (c == child || c.getPrimaryResource == child) {
        it.remove()
      }
    }

    children.find(_.isInstanceOf[TranscodeVirtualFolder]) match {
      case Some(tf) =>
        val tfit = tf.getChildren.listIterator()
        while (tfit.hasNext) {
          val c = tfit.next()
          if (c.getName == child.getName) {
            tfit.remove()
          }
        }
      case None => // No transcode folder, nothing more to do!
    }

    notifyRefresh()
  }

  def publishPhoto(id: String, data: () => InputStream, len: Int) = {
    val resource = new StreamablePhoto(id, data, len)
    val url = publishResource(id, resource)
    if (url.isDefined) {
      debug("Publishing photo with id {} of length {} at {}.", id, len.toString, url)
    }
    url
  }

  def publishMovie(id: String, url: String) = {
    val resource = new WebVideoStream(id, url, null)
    val msUrl = publishResource(id, resource)
    if (msUrl.isDefined) {
      debug("Publishing video with id {} and original URL {} at {}.", id, url, msUrl)
    }
    msUrl
  }

  private def publishResource(id: String, resource: DLNAResource): Option[String] = {
    if (published.contains(id)) {
      unpublish(id)
    }
    published += ((id, resource))
    if (addDynamicResource(resource)) {
      Some(baseUrl + "/get/" + resource.getResourceId + "/" + resource.getName)
    } else {
      None
    }
  }

  def unpublish(id: String) {
    published.get(id) match {
      case Some(res) =>
        debug("Unpublishing photo/video with id {}.", id)
        published -= id
        removeDynamicResource(res)
      case None =>
        throw new IllegalStateException("Not published: " + id)
    }
  }

}