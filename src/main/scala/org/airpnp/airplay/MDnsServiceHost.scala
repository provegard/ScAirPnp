package org.airpnp.airplay

import org.airpnp.Logging
import javax.jmdns.{JmDNS, ServiceInfo}
import org.airpnp.Networking

class MDnsServiceHost extends Logging {
  var jmdns: JmDNS = null

  def start() = {
    val addr = Networking.getInetAddress
    debug("Starting mDNS service host at address {}.", addr);
    jmdns = JmDNS.create(addr)
  }

  def stop() = {
    debug("Stopping mDNS service host.");
    jmdns.unregisterAllServices()
    jmdns.close()
    jmdns = null
  }

  def register(service: ServiceInfo) = {
    requireStarted
    info("Registering mDNS service with name {}.", service.getName())
    jmdns.registerService(service)
  }

  def unregister(service: ServiceInfo) = {
    requireStarted
    info("Unregistering mDNS service with name {}.", service.getName())
    jmdns.unregisterService(service)
  }
  
  def requireStarted() = if (jmdns == null) throw new IllegalStateException("MDnsServiceHost has not been started.");
}