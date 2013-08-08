package org.airpnp

import org.airpnp.dlna.AirPnpFolder
import org.airpnp.dlna.StreamablePhoto
import net.pms.dlna.RealFile
import java.io.File
import net.pms.dlna.WebVideoStream

trait TestMode extends Logging {
  private[airpnp] def maybeAddTestContent(folder: AirPnpFolder) {
    if (!isTestMode) {
      return
    }
    debug("AirPnp test mode detected, adding test content to the AirPnp folder.")
    val photo = new StreamablePhoto({ getClass.getResourceAsStream("/org/airpnp/lena.jpg") }, 27172)
    val video = new WebVideoStream("Video", "http://www.cybertechmedia.com/samples/hunterdouglas.mov", null)
    folder.addDynamicChild(photo)
    folder.addDynamicChild(video)
  }

  private def isTestMode() = new File(System.getProperty("java.io.tmpdir"), "airpnp.test").exists
}