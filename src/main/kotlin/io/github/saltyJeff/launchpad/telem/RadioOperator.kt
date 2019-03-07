package io.github.saltyJeff.launchpad.telem

import com.digi.xbee.api.XBeeDevice
import org.knowm.xchart.QuickChart
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class RadioOperator(port: String) {
    private val device = XBeeDevice(port, 9600)
    private val dataHolder = mutableListOf<String>()
    private var parser: StructParser
    private val logger = LoggerFactory.getLogger("Radio Operator")

    private var startTime: Long = -1
    private var lastTime: Long = 0
    private var thisHz = 0.0
    private var fullHz = 0.0
    private var msgsRead: Long = 0
    private var tsIdx: Int
    private var altIdx: Int

    private val openPlots = mutableMapOf<Int, ChartWrapper>()

    private var lastK = 0
    init {
        logger.info("Preparing radio serial port $port")
        parser = StructParser(Telemetry.fields, dataHolder)
        tsIdx = Telemetry.timestampIdx
        altIdx = Telemetry.altitudeIdx
        logger.debug("TS Index: $tsIdx, ALT Index: $altIdx")
        device.addDataListener {
            Files.write(Telemetry.params.outputFile.toPath(), it.data, StandardOpenOption.APPEND)
            if(parser.parseBytes(it.data)) {
                val thisTime = dataHolder[tsIdx].toLong()
                if(startTime < 0) {
                    startTime = thisTime
                    lastTime = 1
                    thisHz = 1.0
                    fullHz = 1.0
                }
                else {
                    thisHz = 1.0 / ((thisTime - lastTime) / 1000)
                    fullHz = msgsRead.toDouble() / ((thisTime - startTime) / 1000)
                }
                msgsRead++
                lastTime = thisTime

                openPlots.forEach {
                    it.value.chart.updateXYSeries(
                        "series",
                        doubleArrayOf(thisTime / 1000.0),
                        doubleArrayOf(dataHolder[it.key].toDouble()),
                        null)
                    it.value.repaint()
                }

                val thisHeight = (dataHolder[altIdx].toDouble()  / 1000).toInt()
                if(thisHeight > lastK && lastK > 0) {
                    lastK = thisHeight
                    Kevin.speak("$thisHeight meters")
                }
            }
        }
    }
    fun fieldsChanged() {
        parser = StructParser(Telemetry.fields, dataHolder)
        tsIdx = Telemetry.timestampIdx
        altIdx = Telemetry.altitudeIdx
    }
    var streamFrame = StreamFrame(dataHolder)
    fun tglStreamFrame() {
        streamFrame.isVisible = !streamFrame.isVisible
    }
    fun printNetStatistics() {
        logger.info("Full HZ: $fullHz")
        logger.info("This HZ: $thisHz")
        logger.info("Message count: $msgsRead")
    }
    fun startPlotting(idx: Int) {
        val name = Telemetry.fields[idx].fieldName
        if(openPlots.containsKey(idx)) {
            logger.info("Plot for $name is already open")
            return
        }
        var plot = QuickChart.getChart("$name over time", "time", "magnitude", "series", doubleArrayOf(0.0), doubleArrayOf(0.0))
        val wrapper = ChartWrapper(plot)
        openPlots[idx] = wrapper
        wrapper.isVisible = true
    }
}