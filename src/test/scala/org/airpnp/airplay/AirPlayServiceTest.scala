package org.airpnp.airplay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import org.testng.annotations.Test
import java.io.InputStream
import java.net.InetAddress

private class FakeAirPlayDevice(private val name: String, private val udn: String) extends BaseAirPlayDevice(name, udn) {
  def getScrub() = future { new DurationAndPosition(0, 0) }
  def isPlaying() = future { false }
  def setScrub(position: Double) = future { () }
  def play(location: String, position: Double) = future { () }
  def stop() = future { () }
  def showPhoto(data: InputStream, transition: String) = future { () }
  def setRate(rate: Double) = future { () }
  def setProperty(name: String, value: Any) = future { () }
}

class AirPlayServiceTest {

  @Test(groups = Array("Manual"))
  def testRegisterAirPlayService() {

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
