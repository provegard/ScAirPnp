package org.airpnp.plumbing

import org.airpnp.plumbing.Dynamic._
import org.testng.annotations.Test
import org.fest.assertions.Assertions.assertThat
import java.io.IOException

private object Target {
  var staticCalls = 0
  
  def staticMethod() = staticCalls += 1
}

private class Base {
  var baseCalled = 0
  
  def baseMethod() = baseCalled += 1
}

private class Target extends Base {
  var publicCalls = 0
  var privateCalls = 0
  var argType: String = null

  def publicMethod() = publicCalls += 1
  private def privateMethod() = privateCalls += 1
  def methodWithArgs(x: Int) = argType = "Int"
  def methodWithArgs(x: Double) = argType = "Double"
  def doubler(x: Int) = x * 2
  def thrower() = throw new IOException("foo")
}

private class PrivateConstructorTarget private {
}

private class ThrowingConstructorTarget {
  throw new IOException("foo")
}

class DynamicTest {
  
  @Test def shouldInvokePublicMethod(): Unit = {
    val t = new Target

    t.call("publicMethod")

    assertThat(t.publicCalls).isEqualTo(1)
  }

  @Test def shouldInvokePrivateMethod(): Unit = {
    val t = new Target

    t.call("privateMethod")

    assertThat(t.privateCalls).isEqualTo(1)
  }
  
  @Test def shouldInvokeMethodBasedOnParameterTypes(): Unit = {
    val t = new Target

    t.call("methodWithArgs", classOf[Int], 42: java.lang.Integer)

    assertThat(t.argType).isEqualTo("Int")
  }
  
  @Test def shouldSupportReturnValue(): Unit = {
    val t = new Target
    val result = t.call("doubler", classOf[Int], 42: java.lang.Integer)

    assertThat(result).isEqualTo(84: java.lang.Integer)
  }
  
  @Test(expectedExceptions = Array(classOf[IOException]))
  def shouldUnpackExceptions(): Unit = {
    val t = new Target
    t.call("thrower")
  }

  @Test def shouldCreateNewInstance(): Unit = {
    val t = classOf[Target]
    val inst: AnyRef = t createNew;
    assertThat(inst).isInstanceOf(classOf[Target])
  }

  @Test def shouldCreateNewInstanceWithPrivateConstructor(): Unit = {
    val t = classOf[PrivateConstructorTarget]
    val inst: AnyRef = t createNew;
    assertThat(inst).isInstanceOf(classOf[PrivateConstructorTarget])
  }

  @Test(expectedExceptions = Array(classOf[IOException]))
  def shouldUnpackConstructorExceptions(): Unit = {
    val t = classOf[ThrowingConstructorTarget]
    t createNew;
  }

  @Test def shouldInvokeStaticMethod(): Unit = {
    val t = classOf[Target]

    t.callStatic("staticMethod")

    assertThat(Target.staticCalls).isEqualTo(1)
  }

  @Test def shouldInvokeMethodInBaseClass(): Unit = {
    val t = new Target

    t.call("baseMethod")

    assertThat(t.baseCalled).isEqualTo(1)    
  }

  @Test(expectedExceptions = Array(classOf[NoSuchMethodException]))
  def shouldThrowWhenMethodIsNotFoundInClassChain(): Unit = {
    val t = new Target

    t.call("noSuchMethod")
  }

}