package org.airpnp.actor

import scala.actors.Actor._
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.Test
import scala.actors.Actor
import scala.xml.XML
import scala.actors.Exit
import org.fest.assertions.ObjectAssert

class DeviceBuilderTest {

  //  @Test def shouldBeStoppable(): Unit = {
  //    var db = new DeviceBuilder({ null })
  //    db.start()
  //    db !? Stop
  //    assertThat(db.getState).isEqualTo(Actor.State.Terminated)
  //  }

  private def stopper(a: Actor, action: () => Unit) = {
    try {
      action.apply()
    } finally {
      a !? Stop
    }
  }

  @Test def shouldBuildMediaRendererAndReturnDevice(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:67ff722f-0090-a976-17db-e9396986c234", "http://mediarenderer.com")
      assertThat(reply).isInstanceOf(classOf[DeviceReady])
    })
  }

  @Test def shouldInitializeServicesForTheDevice(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:67ff722f-0090-a976-17db-e9396986c234", "http://mediarenderer.com")
      val device = reply.asInstanceOf[DeviceReady].device
      assertThat(device.getServices.head.action("GetCurrentTransportActions").isDefined).isTrue
    })
  }

  @Test def shouldIgnorePrinter(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:06aee441-986a-44af-9d45-2378213b4e60", "http://printer.com")
      assertThat(reply).isInstanceOf(classOf[DeviceShouldBeIgnored])
    })
  }

  @Test def shouldIgnorePrinterWithCorrectUdn(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:06aee441-986a-44af-9d45-2378213b4e60", "http://printer.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.udn).isEqualTo("uuid:06aee441-986a-44af-9d45-2378213b4e60")
    })
  }

  @Test def shouldIgnorePrinterWithReason(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:06aee441-986a-44af-9d45-2378213b4e60", "http://printer.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.reason).contains("not a media renderer")
    })
  }

  @Test def shouldIgnoreDeviceWhenDownloadFails(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:abcdefg", "http://nosuchthing.com")
      assertThat(reply).isInstanceOf(classOf[DeviceShouldBeIgnored])
    })
  }

  @Test def shouldIgnoreDeviceWhenDownloadFailsWithCorrectUdn(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:abcdefg", "http://nosuchthing.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.udn).isEqualTo("uuid:abcdefg")
    })
  }

  @Test def shouldIgnoreDeviceWhenDownloadFailsWithReason(): Unit = {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db, () => {
      val reply = db !? Build("uuid:abcdefg", "http://nosuchthing.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.reason).contains("build error")
    })
  }

}