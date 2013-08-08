package org.airpnp.plumbing;

import org.airpnp.plumbing.Dynamic._
import java.io.IOException
import java.net.DatagramSocket
import java.net.DatagramSocketImpl
import java.net.DatagramSocketImplFactory
import java.net.DatagramPacket
import org.airpnp.Logging

object InterceptingDatagramSocketImplFactory extends Logging {
  def install(interceptor: (DatagramPacket) => Unit) = {
    debug("Installing InterceptingDatagramSocketImplFactory with interceptor.")
    DatagramSocket.setDatagramSocketImplFactory(new InterceptingDatagramSocketImplFactory(interceptor))
  }
}

class InterceptingDatagramSocketImplFactory(private val interceptor: (DatagramPacket) => Unit) extends DatagramSocketImplFactory {

  override def createDatagramSocketImpl(): DatagramSocketImpl = {
    val isMc: java.lang.Boolean = isMulticast
    val real = createTheRealImpl(isMc);
    return new InterceptingDatagramSocketImpl(isMc, real, interceptor)
  }

  private def createTheRealImpl(isMulticast: java.lang.Boolean): DatagramSocketImpl = {
    val defaultFactoryClass = tryClassForName("java.net.DefaultDatagramSocketImplFactory", classOf[Object])
    if (defaultFactoryClass != null) {
      // Java 1.7, just delegate to the default factory. It does NOT
      // implement any interface!
      return defaultFactoryClass.callStatic("createDatagramSocketImpl", classOf[Boolean], isMulticast).asInstanceOf[DatagramSocketImpl]
    }

    // Pre-1.7...
    val plainImplClass = tryClassForName("java.net.PlainDatagramSocketImpl", classOf[DatagramSocketImpl])
    plainImplClass.createNew()
  }

  private def isMulticast(): Boolean = {
    // Major hack, but we cannot know in the factory if the caller wants a multicast
    // or unicast socket. So look for MulticastSocket in the stack frames.
    Thread.currentThread.getStackTrace.slice(3, 10).exists(e => "java.net.MulticastSocket" == e.getClassName())
  }

  private def tryClassForName[T >: Null <: AnyRef](name: String, klass: Class[T]): Class[T] = {
    try {
      return Class.forName(name).asInstanceOf[Class[T]]
    } catch {
      case e: ClassNotFoundException => null
    }
  }
}