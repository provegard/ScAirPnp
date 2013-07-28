package org.airpnp.upnp

import org.fest.assertions.Assertions.assertThat
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import scala.xml.XML

class ActionTest {
  private var action: Action = null

  @BeforeClass
  def createAction(): Unit = {
    val stream = getClass().getResourceAsStream("mediarenderer/service_scpd.xml")
    val root = XML.load(stream)
    val a = (root \\ "actionList" \ "action")(0)

    action = new Action(a)
  }

  @Test
  def shouldHaveAName(): Unit = {
    assertThat(action.getName).isEqualTo("GetCurrentTransportActions")
  }

  @Test
  def shouldHaveInputArguments(): Unit = {
    assertThat(action.inputArguments).isEqualTo(Seq("InstanceID"))
  }

  @Test
  def shouldHaveOutputArguments(): Unit = {
    assertThat(action.outputArguments).isEqualTo(Seq("Actions"))
  }
}