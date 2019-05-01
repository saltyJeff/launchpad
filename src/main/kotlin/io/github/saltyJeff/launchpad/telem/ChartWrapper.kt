package io.github.saltyJeff.launchpad.telem

import org.knowm.xchart.QuickChart
import org.knowm.xchart.XChartPanel
import org.knowm.xchart.XYChart
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import javax.swing.JFrame
import javax.swing.JPanel

class ChartWrapper(private val variable: String, frames: Int = 150): JFrame("$variable Chart"), WindowListener {
    val xArray = DoubleArray(frames)
    val yArray = DoubleArray(frames)
    private val chart = QuickChart.getChart("$variable over time", "time", "magnitude", variable, xArray, yArray)
    val panel = XChartPanel<XYChart>(chart)
    var closed = false
    init {
        add(panel)
        addWindowListener(this)
        pack()
    }
    fun addData(x: Double, y: Double) {
        System.arraycopy(xArray, 1, xArray, 0, xArray.size - 1)
        System.arraycopy(yArray, 1, yArray, 0, yArray.size - 1)
        xArray[xArray.lastIndex] = x
        yArray[yArray.lastIndex] = y
        if(xArray[0].toLong() == 0L) {
            xArray.fill(x)
            yArray.fill(y)
        }
        panel.chart.updateXYSeries(variable, xArray, yArray, null)
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