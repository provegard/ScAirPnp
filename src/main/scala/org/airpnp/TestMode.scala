package org.airpnp

import java.io.File

import org.airpnp.dlna.AirPnpFolder

trait TestMode extends Logging {
  private[airpnp] def maybeAddTestContent(folder: AirPnpFolder) {
    if (!isTestMode) {
      return
    }
    debug("AirPnp test mode detected, adding test content to the AirPnp folder.")
    folder.publishPhoto("photo1", () => getClass.getResourceAsStream("/org/airpnp/lena.jpg"), 27172)
    folder.publishMovie("video1", "http://www.cybertechmedia.com/samples/hunterdouglas.mov")
  }

  private def isTestMode() = new File(System.getProperty("java.io.tmpdir"), "airpnp.test").exists
}