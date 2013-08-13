package org.airpnp.actor

import java.io.File
import org.airpnp.dlna.DLNAPublisher
import org.airpnp.Logging

trait TestMode extends Logging {
  private[actor] def maybeAddTestContent(publisher: DLNAPublisher) {
    if (!isTestMode) {
      return
    }
    debug("AirPnp test mode detected, publishing test content.")
    val photoUrl = publisher.publishPhoto("photo1", () => getClass.getResourceAsStream("/org/airpnp/lena.jpg"), 27172)
    debug("-- photo URL is: {}", photoUrl)
    val videoUrl = publisher.publishMovie("video1", "http://www.cybertechmedia.com/samples/hunterdouglas.mov")
    debug("-- video URL is: {}", videoUrl)
  }

  private def isTestMode() = new File(System.getProperty("java.io.tmpdir"), "airpnp.test").exists
}