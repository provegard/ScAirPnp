package org.airpnp.actor

import scala.actors.Actor
import scala.actors.Actor._
import org.airpnp.Logging
import scala.collection.mutable.MutableList
import org.airpnp.upnp.Device
import scala.collection.mutable.HashMap
import scala.actors.Exit
import java.util.concurrent.CountDownLatch
import scala.collection.mutable.HashSet
import org.airpnp.airplay.MDnsServiceHost
import java.net.InetAddress

class CoordinatorOptions {
  var deviceBuilder: Actor = null
  var deviceDiscovery: Actor = null
  var devicePublisher: Actor = null
  //  var checkLiveness: Actor = null
  var discoveryInterval = 300000l
  var livenessCheckInterval = 30000l
  var address: InetAddress = null
}

class Coordinator(private val options: CoordinatorOptions) extends Actor with Logging {
  private val deviceBuilder = options.deviceBuilder
  private val deviceDiscovery = options.deviceDiscovery
  private val devicePublisher = options.devicePublisher

  private val foundUdns = new HashSet[String]()
  private val ignoredUdns = new HashSet[String]()
  private val depActors = Seq(deviceBuilder, deviceDiscovery, devicePublisher)

  private var stopCount = 0

  private val schedulers = Seq(
    Scheduling.scheduler(1000, options.discoveryInterval) {
      if (deviceDiscovery != null) {
        deviceDiscovery ! DoDiscovery
      }
    },
    Scheduling.scheduler(options.livenessCheckInterval, options.livenessCheckInterval) {
      //      if (checkLiveness != null) {
      //        checkLiveness ! DoLivenessCheck(devices.values.toList)
      //      }
    })

  override def start(): Actor = {
    depActors.filter(_ != null).foreach(_.start())
    schedulers.foreach(_.start())
    super.start()
  }

  def act() = {
    loop {
      react {
        case df: DeviceFound =>
          if (!ignoredUdns.contains(df.udn)) {
            if (!foundUdns.contains(df.udn)) {
              foundUdns += df.udn
              deviceBuilder ! Build(df.udn, df.location)
            } //TODO: else, perhaps ping/touch??
          }

        case ign: DeviceShouldBeIgnored =>
          ign.device match {
            case Some(d) => debug("Ignoring device '{}' because: {}.", d.getFriendlyName, ign.reason)
            case None => debug("Ignoring device with UDN {} because: {}.", ign.udn, ign.reason)
          }
          ignoredUdns += ign.udn //TODO: What do we need it for now that we have foundUdns??

        case dr: DeviceReady =>
          devicePublisher ! Publish(dr.device)

        case Stop =>
          //TODO: Waitall!
          schedulers.foreach(_ ! Stop)
          depActors.filter(_ != null).foreach(_ ! Stop)
          debug("Coordinator was stopped.")
          sender ! Stopped
          exit
      }
    }
  }
}