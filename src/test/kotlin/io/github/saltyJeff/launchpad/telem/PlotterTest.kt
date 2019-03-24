package io.github.saltyJeff.launchpad.telem

import org.junit.jupiter.api.Test
import org.knowm.xchart.QuickChart

class PlotterTest {
    @Test
    fun `Plot stuff in real time`() {
        val frame = ChartWrapper("sine")
        frame.isVisible = true
        var lastTime: Long = 0
        var x = 0
        while(frame.isVisible) {
            if(lastTime + 50 < System.currentTimeMillis()) {
                frame.addData(x++.toDouble(), Math.sin(x.toDouble()))
                lastTime = System.currentTimeMillis()
            }
        }
    }
}