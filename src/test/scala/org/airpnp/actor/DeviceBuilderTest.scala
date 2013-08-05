package org.airpnp.actor

import org.airpnp.actor.ActorTestUtil._
import scala.actors.Actor._
import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.Test
import scala.actors.Actor
import scala.xml.XML
import scala.actors.Exit
import org.fest.assertions.ObjectAssert

class DeviceBuilderTest {

  //  @Test def shouldBeStoppable() {
  //    var db = new DeviceBuilder({ null })
  //    db.start()
  //    db !? Stop
  //    assertThat(db.getState).isEqualTo(Actor.State.Terminated)
  //  }

  @Test def shouldBuildMediaRendererAndReturnDevice() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:67ff722f-0090-a976-17db-e9396986c234", "http://mediarenderer.com")
      assertThat(reply).isInstanceOf(classOf[DeviceReady])
    }
  }

  @Test def shouldInitializeServicesForTheDevice() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:67ff722f-0090-a976-17db-e9396986c234", "http://mediarenderer.com")
      val device = reply.asInstanceOf[DeviceReady].device
      assertThat(device.getServices.head.action("GetCurrentTransportActions").isDefined).isTrue
    }
  }

  @Test def shouldIgnorePrinter() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:06aee441-986a-44af-9d45-2378213b4e60", "http://printer.com")
      assertThat(reply).isInstanceOf(classOf[DeviceShouldBeIgnored])
    }
  }

  @Test def shouldIgnorePrinterWithCorrectUdn() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:06aee441-986a-44af-9d45-2378213b4e60", "http://printer.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.udn).isEqualTo("uuid:06aee441-986a-44af-9d45-2378213b4e60")
    }
  }

  @Test def shouldIgnorePrinterWithReason() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:06aee441-986a-44af-9d45-2378213b4e60", "http://printer.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.reason).contains("not a media renderer")
    }
  }

  @Test def shouldIgnoreDeviceWhenDownloadFails() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:abcdefg", "http://nosuchthing.com")
      assertThat(reply).isInstanceOf(classOf[DeviceShouldBeIgnored])
    }
  }

  @Test def shouldIgnoreDeviceWhenDownloadFailsWithCorrectUdn() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:abcdefg", "http://nosuchthing.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.udn).isEqualTo("uuid:abcdefg")
    }
  }

  @Test def shouldIgnoreDeviceWhenDownloadFailsWithReason() {
    val db = new DeviceBuilder(Downloader.create())
    db.start()
    stopper(db) {
      val reply = db !? Build("uuid:abcdefg", "http://nosuchthing.com")
      val ignored = reply.asInstanceOf[DeviceShouldBeIgnored]
      assertThat(ignored.reason).contains("build error")
    }
  }

}