package org.airpnp.actor

import scala.concurrent.ExecutionContext.Implicits.global
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
import scala.util.Success
import scala.util.Failure
import org.airpnp.dlna.DLNAPublisher
import scala.concurrent.Promise

class CoordinatorOptions {
  var deviceBuilder: Actor = null
  var deviceDiscovery: Actor = null
  var devicePublisher: Actor = null
  var dlnaPublisher: Actor = null
  //  var checkLiveness: Actor = null
  var discoveryInterval = 300000l
  var livenessCheckInterval = 30000l
  var address: InetAddress = null
}

class Coordinator(options: CoordinatorOptions) extends BaseActor with TestMode {
  private val deviceBuilder = options.deviceBuilder
  private val deviceDiscovery = options.deviceDiscovery
  private val devicePublisher = options.devicePublisher

  private val foundUdns = new HashSet[String]()
  private val ignoredUdns = new HashSet[String]()
  private val depActors = Seq(deviceBuilder, deviceDiscovery, devicePublisher,
    options.dlnaPublisher)

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

  override def toString() = "Coordinator"

  override def start(): Actor = {
    depActors.filter(_ != null).foreach(_.start())
    schedulers.foreach(_.start())
    super.start()
  }

  def act() = {
    loop {
      react {
        case msg: GetPublishedDevices =>
          devicePublisher.forward(msg)
          
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

        case TriggerPMSFolderDiscovery(udn, location, promise) =>
          trace("Will trigger PMS folder discovery.")
          val reply = deviceBuilder !? Build(udn, location, true)
          reply match {
            case DeviceReady(device) =>
              val s = sender
              forceFolderDiscovery(device, promise)
            case x: DeviceShouldBeIgnored =>
              error("Cannot force PMS folder directory because: {}", x.reason)
              sender ! promise.failure(null)
          }

        case MaybePublishTestContent =>
          maybeAddTestContent(options.dlnaPublisher.asInstanceOf[DLNAPublisher])

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

  private def forceFolderDiscovery(device: Device, promise: Promise[Unit]) {
    debug("Forcing PMS folder discovery.")

    device.getServiceById("urn:upnp-org:serviceId:ContentDirectory") match {
      case Some(service) =>
        service.action("Browse") match {
          case Some(action) =>

            val comm = new DeviceCommunicator(device)
            comm.start()
            val sender = comm.createSoapSender()

            val msg = action.createSoapMessage(("ObjectID", "0"),
              ("BrowseFlag", "BrowseDirectChildren"),
              ("Filter", "*"),
              ("StartingIndex", "0"),
              ("RequestedCount", "0"), // all
              ("SortCriteria", "")) // no sorting
            sender(service.getControlURL, msg).onComplete {
              case Success(reply) =>
                trace("Got successful browse reply from PMS.")
                comm !? Stop
                promise.success(())
              case Failure(t) =>
                error("Failed to browse PMS's root folder.", t)
                comm !? Stop
                promise.failure(t)
            }
          case None =>
            error("PMS's ContentDirectory service misses the Browse action.")
            promise.failure(null)
        }
      case None =>
        error("PMS misses the ContentDirectory service.")
        promise.failure(null)
    }
  }
}