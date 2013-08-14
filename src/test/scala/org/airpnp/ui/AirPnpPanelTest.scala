package org.airpnp.ui

import org.testng.annotations.Test
import javax.swing.JOptionPane
import java.awt.Dimension
import javax.swing.SwingUtilities

class AirPnpPanelTest {
  @Test(groups = Array("Manual")) def testPanelWithTestDevice() {
    val device = org.airpnp.upnp.buildInitializedMediaRenderer("http://base.com")
    val panel = new AirPnpPanel(Array(device))
    SwingUtilities.invokeAndWait(new Runnable {
      def run() {
        JOptionPane.showOptionDialog(
          null,
          panel,
          "Test panel",
          JOptionPane.CLOSED_OPTION,
          JOptionPane.PLAIN_MESSAGE, null, null, null)
      }
    })
  }
}