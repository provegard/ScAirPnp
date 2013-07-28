package org.airpnp.plumbing;

import scala.language.implicitConversions
import java.net.DatagramPacket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import org.airpnp.plumbing.Dynamic.anyref2callable

class InterceptingDatagramSocketImpl(val isMulticast: Boolean,
  private val delegate: DatagramSocketImpl,
  private val interceptor: (DatagramPacket) => Unit) extends DatagramSocketImpl {

  implicit def unpackInt(x: AnyRef): Int = x.asInstanceOf[java.lang.Integer]
  implicit def unpackByte(x: AnyRef): Byte = x.asInstanceOf[java.lang.Byte]

  override def setOption(optID: Int, value: AnyRef) = delegate.setOption(optID, value)
  override def getOption(optID: Int) = delegate.getOption(optID)
  protected def create() = delegate.call("create")

  protected def bind(lport: Int, laddr: InetAddress) = {
    delegate.call("bind", classOf[Int], classOf[InetAddress], lport: java.lang.Integer, laddr)
  }

  protected def send(p: DatagramPacket) = delegate.call("send", classOf[DatagramPacket], p)
  protected def peek(i: InetAddress): Int = delegate.call("peek", classOf[InetAddress], i)
  protected def peekData(p: DatagramPacket): Int = delegate.call("peekData", classOf[DatagramPacket], p)

  protected def receive(p: DatagramPacket) = {
    delegate.call("receive", classOf[DatagramPacket], p)
    interceptor(p)
  }

  protected def setTTL(ttl: Byte) = delegate.call("setTTL", classOf[Byte], ttl: java.lang.Byte)
  protected def getTTL(): Byte = delegate.call("getTTL")

  protected def setTimeToLive(ttl: Int) = delegate.call("setTimeToLive", classOf[Int], ttl: java.lang.Integer)
  protected def getTimeToLive(): Int = delegate.call("getTimeToLive")

  protected def join(inetaddr: InetAddress) = delegate.call("join", classOf[InetAddress], inetaddr)
  protected def leave(inetaddr: InetAddress) = delegate.call("leave", classOf[InetAddress], inetaddr)

  protected def joinGroup(mcastaddr: SocketAddress, netIf: NetworkInterface) = {
    delegate.call("joinGroup", classOf[SocketAddress], classOf[NetworkInterface], mcastaddr, netIf)
  }

  protected def leaveGroup(mcastaddr: SocketAddress, netIf: NetworkInterface) = {
    delegate.call("leaveGroup", classOf[SocketAddress], classOf[NetworkInterface], mcastaddr, netIf)
  }

  protected def close() = delegate.call("close")
}
