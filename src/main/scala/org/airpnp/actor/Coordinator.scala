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
//  private val checkLiveness = options.checkLiveness

  private val ignoredUdns = new HashSet[String]()
//  private val publishers = new HashMap[String, Actor]()
//  private val devices = new HashMap[String, Device]()
  private val depActors = Seq(deviceBuilder, deviceDiscovery, devicePublisher)
//  private val mdnsHost = new MDnsServiceHost()

  private var stopCount = 0

  private val schedulers = Seq(
    Scheduling.scheduler(options.discoveryInterval) {
      if (deviceDiscovery != null) {
        deviceDiscovery ! DoDiscovery
      }
    },
    Scheduling.scheduler(options.livenessCheckInterval) {
      //      if (checkLiveness != null) {
      //        checkLiveness ! DoLivenessCheck(devices.values.toList)
      //      }
    })

  override def start(): Actor = {
//    mdnsHost.start(options.address)
    depActors.filter(_ != null).foreach(_.start())
    schedulers.foreach(_.start())
    super.start()
  }

  def act() = {
    loop {
      react {
        case df: DeviceFound => {
          if (!ignoredUdns.contains(df.udn)) {
            deviceBuilder ! Build(df.udn, df.location)
          }
        }

        case ign: DeviceShouldBeIgnored => {
          debug("Ignoring device with UDN {} because {}.", ign.udn, ign.reason)
          ignoredUdns += ign.udn
        }

        case dr: DeviceReady => {
//          val udn = dr.device.getUdn
//          devices += ((udn, dr.device))
//          val publisher = new DevicePublisher(mdnsHost, options.address, dr.device)
//          publishers += ((udn, publisher))
//          publisher.start()
          devicePublisher ! Publish(dr.device)
          //TODO: Log, create and send to DevicePublisher
        }

        case Stop => {
          //TODO: Waitall!
          schedulers.foreach(_ ! Stop)
          depActors.filter(_ != null).foreach(_ ! Stop)
//          publishers.values.toList.foreach(_ ! Stop)
//          mdnsHost.stop()
          
          debug("Coordinator was stopped.")
          sender ! Stopped
          exit
        }
      }
    }
  }
}