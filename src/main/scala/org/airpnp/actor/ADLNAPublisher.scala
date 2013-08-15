package org.airpnp.actor

import scala.actors.Actor
import java.io.InputStream
import org.airpnp.Logging
import org.airpnp.dlna.DLNAPublisher

private case class PublishPhoto(id: String, data: () => InputStream, len: Int)
private case class PublishMovie(id: String, url: String)
private case class Unpublish(id: String)
private case class Done(ret: Option[String])

class ADLNAPublisher(delegee: org.airpnp.dlna.DLNAPublisher) extends BaseActor with DLNAPublisher { self =>

  def publishPhoto(id: String, data: () => InputStream, len: Int) = {
    ADLNAPublisher.this !? PublishPhoto(id, data, len) match {
      case Done(url) => url
      case _ => throw new IllegalStateException("Unknown reply from PublishPhoto.")
    }
  }

  def publishMovie(id: String, url: String) = {
    ADLNAPublisher.this !? PublishMovie(id, url) match {
      case Done(url) => url
      case _ => throw new IllegalStateException("Unknown reply from PublishMovie.")
    }

  }

  def unpublish(id: String) {
    ADLNAPublisher.this !? Unpublish(id)
  }

  def act() {
    loop {
      react {
        case pp: PublishPhoto =>
          val url = delegee.publishPhoto(pp.id, () => pp.data(), pp.len)
          sender ! Done(url)

        case pm: PublishMovie =>
          val url = delegee.publishMovie(pm.id, pm.url)
          sender ! Done(url)

        case up: Unpublish =>
          delegee.unpublish(up.id)
          sender ! Done(Some(""))
          
        case Stop =>
          debug("DLNA publisher was stopped.")
          sender ! Stopped
          exit
      }
    }
  }
}