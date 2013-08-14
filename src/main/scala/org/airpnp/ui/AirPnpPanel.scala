package org.airpnp.ui

import javax.swing.JPanel
import java.awt.BorderLayout
import org.airpnp.upnp.Device
import javax.swing.JLabel
import javax.swing.JTree
import javax.swing.tree.TreeModel
import javax.swing.JSplitPane
import javax.swing.tree.DefaultTreeModel
import java.awt.Dimension
import javax.swing.event.TreeModelListener
import java.awt.Container
import javax.swing.event.TreeSelectionListener
import javax.swing.event.TreeSelectionEvent

object AirPnpPanel {
  val PREFERRED_HEIGHT = 600
  val PREFERRED_WIDTH = 800
}

class AirPnpPanel(devices: Seq[Device]) extends JPanel {

  setLayout(new BorderLayout)
  add({
    val right = new JPanel()
    right.setLayout(new BorderLayout())
    val left = new JTree(createTreeModel)
    left.addTreeSelectionListener(new SelectionListener(right))

    val pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right)
    pane.setResizeWeight(0.3)
    pane
  }, BorderLayout.CENTER)
  setPreferredSize(new Dimension(AirPnpPanel.PREFERRED_WIDTH, AirPnpPanel.PREFERRED_HEIGHT))

  private def createTreeModel = new DefaultTreeModel(new RootNode(devices))

  private class SelectionListener(c: Container) extends TreeSelectionListener {
    def valueChanged(e: TreeSelectionEvent) {
      c.removeAll()
      val path = e.getNewLeadSelectionPath
      if (path != null) {
        path.getLastPathComponent match {
          case node: MasterNode =>
            val page = node.getDetailPage
            if (page != null) {
              c.add(page, BorderLayout.NORTH)
            }
          case _ =>
        }
      }
      c.revalidate()
      c.repaint()
    }
  }
}