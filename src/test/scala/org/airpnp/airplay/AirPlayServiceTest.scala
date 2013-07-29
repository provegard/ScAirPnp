package org.airpnp.airplay

import org.testng.annotations.Test
import java.io.InputStream
import java.net.InetAddress

private class FakeAirPlayDevice(private val name: String, private val udn: String) extends BaseAirPlayDevice(name, udn) {
  def getScrub() = new DurationAndPosition(0, 0)
  def isPlaying() = false
  def setScrub(position: Double) = ()
  def play(location: String, position: Double) = ()
  def stop() = ()
  def showPhoto(data: InputStream, transition: String) = ()
  def setRate(rate: Double) = ()
  def setProperty(name: String, value: Any) = ()
}

class AirPlayServiceTest {

  @Test(groups = Array("Manual"))
  def testRegisterAirPlayService(): Unit = {

    val apDevice = new FakeAirPlayDevice("AirPlayServiceTest", "uuid:abcdefghijklmn")

    println("Setting up the AirPlay service...")
    var service: AirPlayService = null
    val host = new MDnsServiceHost
    host.start(InetAddress.getLocalHost)
    try {
      service = new AirPlayService(apDevice, 22555)
      host.register(service.getService)

      println("Press ENTER to stop the test.")
      System.in.read()
    } finally {
      if (service != null) {
        host.unregister(service.getService)
      }
      host.stop
    }
  }

}
