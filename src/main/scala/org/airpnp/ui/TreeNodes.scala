package org.airpnp.ui

import scala.concurrent.ExecutionContext.Implicits.global
import org.airpnp.upnp.Device
import javax.swing.tree.DefaultMutableTreeNode
import org.airpnp.upnp.Service
import org.airpnp.upnp.Action
import javax.swing.JComponent
import javax.swing.JPanel
import java.awt.GridBagLayout
import scala.collection.mutable.MutableList
import javax.swing.JTextField
import java.awt.GridBagConstraints
import javax.swing.JLabel
import scala.collection.mutable.HashMap
import javax.swing.JButton
import javax.swing.JTextArea
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.JScrollPane
import java.awt.Container
import java.awt.BorderLayout
import scala.concurrent.Future
import org.airpnp.upnp.SoapMessage
import scala.util.Success
import scala.util.Failure
import org.airpnp.upnp.SoapError
import javax.swing.ScrollPaneConstants
import java.awt.Insets
import org.airpnp.Logging
import java.awt.Dimension

abstract class MasterNode extends DefaultMutableTreeNode {
  def getDetailPage(): JComponent
}

class RootNode(devices: Seq[Device]) extends MasterNode {
  devices.foreach(d => add(new DeviceNode(d)))

  override def toString() = "Published devices"
  def getDetailPage() = null
}

class DeviceNode(device: Device) extends MasterNode {
  device.getServices.foreach(s => add(new ServiceNode(s, device.soapSender)))

  override def toString() = device.getFriendlyName
  def getDetailPage() = null
}

class ServiceNode(service: Service, sender: (String, SoapMessage) => Future[SoapMessage]) extends MasterNode {
  service.getActions.sortBy(_.getName).foreach(a => add(new ActionNode(a, msg => {
    sender(service.getControlURL, msg)
  })))

  override def toString() = service.getServiceId
  def getDetailPage() = null
}

class ActionNode(action: Action, sender: SoapMessage => Future[SoapMessage]) extends MasterNode {
  private var detail: ActionForm = null
  override def toString() = action.getName
  def getDetailPage() = {
    if (detail == null) {
      detail = new ActionForm(action, sender)
    } else {
      detail.clear()
    }
    detail
  }
}

class ActionForm private[ui] (action: Action, sender: SoapMessage => Future[SoapMessage]) extends JPanel with ActionListener with Logging {
  private val inFields = new HashMap[String, JTextField]()
  private val outFields = new HashMap[String, JTextField]()

  private val panel = new JPanel()
  panel.setFont({
    val font = getFont()
    font.deriveFont(font.getSize2D() + 4f)
  })

  private val send = new JButton("Send")
  send.addActionListener(this)
  private val status = new JTextField()
  status.setEditable(false)

  panel.setLayout(new GridBagLayout())
  addComponents(panel)

  setLayout(new BorderLayout())
  add(new JScrollPane(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
    BorderLayout.CENTER)

  setPreferredSize(new Dimension(AirPnpPanel.PREFERRED_WIDTH / 2, AirPnpPanel.PREFERRED_HEIGHT))

  private def clearOut() {
    outFields.foreach(_._2.setText(""))
    status.setText("")
  }

  def clear() {
    inFields.foreach(_._2.setText(""))
    clearOut()
  }

  private def addComponents(c: Container) {
    val inargCount = action.inputArguments.size
    val outargCount = action.outputArguments.size
    val insets = new Insets(2, 2, 4, 2)
    for ((arg, i) <- action.inputArguments.view.zipWithIndex) {
      val gbc = new GridBagConstraints()
      gbc.weightx = 1
      gbc.gridy = i * 2
      gbc.insets = insets
      gbc.fill = GridBagConstraints.HORIZONTAL
      c.add(new JLabel(arg + " [IN]"), gbc)
      gbc.gridy += 1
      val field = new JTextField()
      inFields += ((arg, field))
      c.add(field, gbc)
    }

    var gbc = new GridBagConstraints()
    gbc.gridy = inargCount * 2
    gbc.insets = insets
    gbc.anchor = GridBagConstraints.WEST

    c.add(send, gbc)

    gbc = new GridBagConstraints()
    gbc.weightx = 1
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.gridy = inargCount * 2 + 1
    gbc.insets = insets
    if (outargCount == 0) {
      // Last component, let it expand downwards.
      gbc.weighty = 1
      gbc.anchor = GridBagConstraints.NORTHWEST
    }
    c.add(status, gbc)

    for ((arg, i) <- action.outputArguments.view.zipWithIndex) {
      val gbc = new GridBagConstraints()
      gbc.weightx = 1
      gbc.gridy = inargCount * 2 + 2 + i * 2
      gbc.fill = GridBagConstraints.HORIZONTAL
      gbc.insets = insets
      c.add(new JLabel(arg + " [OUT]"), gbc)
      gbc.gridy += 1

      if (i == outargCount - 1) {
        // Last component, let it expand downwards.
        gbc.weighty = 1
        gbc.anchor = GridBagConstraints.NORTHWEST
      }

      val field = new JTextField()
      field.setEditable(false)
      outFields += ((arg, field))
      c.add(field, gbc)
    }

    c.getComponents.foreach(_.setFont(c.getFont))
  }

  override def actionPerformed(e: ActionEvent) {
    val args = inFields.toSeq.map(t => (t._1, t._2.getText))
    val msg = action.createSoapMessage(args: _*)
    clearOut()
    send.setEnabled(false)
    sender(msg).andThen({
      case Success(reply) =>
        status.setText("OK")
        trace("SOAP reply:\n{}", reply.toString) //TODO: pretty
        outFields.foreach(f => f._2.setText(reply.getArgument(f._1, "")))
      case Failure(err: SoapError) =>
        status.setText("SOAP error: " + err.getMessage)
        trace("SOAP error:\n{}", err.xml)
      case Failure(other) =>
        status.setText("Error: [" + other.getClass.getSimpleName + "] " + other.getMessage)
    }).andThen { case _ => send.setEnabled(true) }
  }
}