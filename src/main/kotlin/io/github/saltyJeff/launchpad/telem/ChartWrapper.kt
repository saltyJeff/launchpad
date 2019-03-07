package io.github.saltyJeff.launchpad.telem

import org.knowm.xchart.XChartPanel
import org.knowm.xchart.XYChart
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JFrame
import javax.swing.JPanel

class ChartWrapper(val chart: XYChart): JFrame("Rocket Chart"), WindowListener {
    val panel: JPanel
    var closed = false
    init {
        panel = XChartPanel<XYChart>(chart)
        add(panel)
        addWindowListener(this)
        pack()
    }
    override fun repaint() {
        panel.revalidate()
        panel.repaint()
    }
    override fun windowClosed(e: WindowEvent?) {
        closed = true
    }

    //irrelevant functions
    override fun windowOpened(e: WindowEvent?) {

    }
    override fun windowClosing(e: WindowEvent?) {

    }
    override fun windowIconified(e: WindowEvent?) {

    }
    override fun windowDeiconified(e: WindowEvent?) {

    }
    override fun windowActivated(e: WindowEvent?) {

    }
    override fun windowDeactivated(e: WindowEvent?) {

    }
}