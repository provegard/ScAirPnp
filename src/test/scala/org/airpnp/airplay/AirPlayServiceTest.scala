package org.airpnp.airplay

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.future
import org.testng.annotations.Test
import java.io.InputStream
import java.net.InetAddress
import org.airpnp.Util
import org.airpnp.airplay.protocol.AirPlayHttpServer
import java.net.InetSocketAddress
import org.slf4j.LoggerFactory

private class FakeAirPlayDevice(name: String, udn: String) extends BaseAirPlayDevice(name, udn) {
  def getScrub() = {
    println("getScrub")
    future { new DurationAndPosition(0, 0) }
  }
  def isPlaying() = {
    println("isPlaying")
    future { false }
  }
  def setScrub(position: Double) = {
    println("setScrub(" + position + ")")
    future { () }
  }
  def play(location: String, position: Double) = {
    println("play('" + location + "', " + position + ")")
    future { () }
  }
  def stop() = {
    println("stop")
    future { () }
  }
  def showPhoto(data: InputStream, length: Int, transition: String) = {
    println("showPhoto(..., " + length + ", " + transition + ")")
    future { () }
  }
  def setRate(rate: Double) = {
    println("setRate(" + rate + ")")
    future { () }
  }
  def setProperty(name: String, value: Any) = {
    println("setProperty('" + name + "', " + value.toString + ")")
    future { () }
  }
}

class AirPlayServiceTest {

  @Test(groups = Array("Manual"))
  def testRegisterAirPlayService() {
    val logger = LoggerFactory.getLogger("org.airpnp").asInstanceOf[ch.qos.logback.classic.Logger]
    logger.setLevel(ch.qos.logback.classic.Level.TRACE)
    
    val port = Util.findPort()
    val addr = InetAddress.getLocalHost
    val apDevice = new FakeAirPlayDevice("AirPlayServiceTest", "uuid:abcdefghijklmn")

    println("Setting up the AirPlay service...")
    var service: AirPlayService = null
    var httpServer: AirPlayHttpServer = null
    val host = new DefaultMDnsServiceHost
    host.start(addr)
    try {
      httpServer = new AirPlayHttpServer(new InetSocketAddress(addr, port), apDevice)
      httpServer.start()
      service = new AirPlayService(apDevice, port)
      host.register(service.getService)

      println("Press ENTER to stop the test.")
      System.in.read()
    } finally {
      if (httpServer != null) {
        httpServer.stop(0)
      }
      if (service != null) {
        host.unregister(service.getService)
      }
      host.stop
    }
  }

}
