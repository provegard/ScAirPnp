package org.airpnp.plumbing

import scala.language.implicitConversions
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

private object Caller {

  private def findMethod(klass: Class[_], methodName: String, argtypes: Seq[Class[_]]): Method = {
    if (klass == null) {
      throw new NoSuchMethodException("Method not found: " + methodName)
    }
    try {
      klass.getDeclaredMethod(methodName, argtypes: _*)
    } catch {
      case e: NoSuchMethodException => findMethod(klass.getSuperclass, methodName, argtypes)
    }
  }

  def call(klass: Class[_], obj: AnyRef, methodName: String, typesAndArgs: AnyRef*): AnyRef = {
    val argtypes = typesAndArgs.slice(0, typesAndArgs.length / 2).map(_.asInstanceOf[Class[_]])
    val args = typesAndArgs.slice(typesAndArgs.length / 2, typesAndArgs.length)
    val method = findMethod(klass, methodName, argtypes)

    val oldAccessible = method.isAccessible
    try {
      method.setAccessible(true)
      method.invoke(obj, args: _*)
    } catch {
      case ite: InvocationTargetException => throw ite.getCause
      case e: Exception => throw e
    } finally {
      method.setAccessible(oldAccessible)
    }
  }
}

case class Caller[T >: Null <: AnyRef](obj: T) {
  def call(methodName: String, typesAndArgs: AnyRef*): AnyRef = {
    Caller.call(obj.getClass, obj, methodName, typesAndArgs: _*)
  }
}

case class Creator[T >: Null <: AnyRef](klass: Class[T]) {
  def createNew(): T = {
    val constr = klass.getDeclaredConstructor()
    val oldAccessible = constr.isAccessible
    try {
      constr.setAccessible(true)
      constr.newInstance()
    } catch {
      case ite: InvocationTargetException => throw ite.getCause
      case e: Exception => throw e
    } finally {
      constr.setAccessible(oldAccessible)
    }
  }

  def callStatic(methodName: String, typesAndArgs: AnyRef*): AnyRef = {
    Caller.call(klass, null, methodName, typesAndArgs: _*)
  }
}

object Dynamic {
  implicit def anyref2callable[T >: Null <: AnyRef](obj: T): Caller[T] = new Caller(obj)
  implicit def class2creatable[T >: Null <: AnyRef](klass: Class[T]): Creator[T] = new Creator(klass)
}

