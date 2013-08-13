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

  private def addDynamicResource(child: DLNAResource) {
    val sizeBefore = getChildren().size
    addChild(child)
    if (sizeBefore == getChildren().size) {
      throw new IllegalStateException("Failed to add DLNA resource: " + child)
    }
    notifyRefresh()
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

  def publishPhoto(id: String, data: () => InputStream, len: Int): String = {
    val resource = new StreamablePhoto(id, data, len)
    val url = publishResource(id, resource)
    debug("Publishing photo with id {} of length {} at {}.", id, len.toString, url)
    url
  }

  def publishMovie(id: String, url: String): String = {
    val resource = new WebVideoStream(id, url, null)
    val msUrl = publishResource(id, resource)
    debug("Publishing video with id {} and original URL {} at {}.", id, url, msUrl)
    msUrl
  }

  private def publishResource(id: String, resource: DLNAResource): String = {
    if (published.contains(id)) {
      throw new IllegalStateException("Alread published: " + id)
    }
    published += ((id, resource))
    addDynamicResource(resource)
    baseUrl + "/get/" + resource.getResourceId + "/" + resource.getName
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