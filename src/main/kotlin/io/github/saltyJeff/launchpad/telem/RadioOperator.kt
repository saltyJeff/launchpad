package io.github.saltyJeff.launchpad.telem

import com.digi.xbee.api.XBeeDevice
import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.MessageParser
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class RadioOperator(port: String, fields: List<CField>, tsIdx: Int, altIdx: Int) {
    private val device = XBeeDevice(port, 9600)
    private val dataHolder = mutableListOf<String>()
    private lateinit var parser: MessageParser
    private val logger = LoggerFactory.getLogger("Radio Operator")

    //network statistics
    private val netStats = NetworkStatistics()

    //indicies of note
    private var tsIdx: Int = -1
    private var altIdx: Int = -1

    //gui holders
    private val openPlots = mutableMapOf<Int, ChartWrapper>()
    var streamFrame = StreamFrame(dataHolder)

    private var lastK = 0
    init {
        logger.info("Preparing radio serial port $port")

        fieldsChanged(fields, tsIdx, altIdx)

        logger.debug("TS Index: $tsIdx, ALT Index: $altIdx")
        device.addDataListener {
            Files.write(Telemetry.params.outputFile.toPath(), it.data, StandardOpenOption.APPEND)
            if(parser.parseBytes(it.data)) {
                val thisTime = dataHolder[tsIdx].toLong()
                netStats.msgReceived(thisTime)

                openPlots.entries.forEach {(i, frame) ->
                    if(frame.closed) {
                        openPlots.remove(i)
                        return@forEach
                    }
                    frame.addData(thisTime / 1000.0, dataHolder[i].toDouble())
                }
                if(tsIdx >= 0) {
                    val thisHeight = (dataHolder[altIdx].toDouble()  / 1000).toInt()
                    if(thisHeight > lastK && lastK > 0) {
                        lastK = thisHeight
                        Kevin.speak("$thisHeight meters")
                    }
                }
            }
        }
    }
    fun fieldsChanged(fields: List<CField>, tsIdx: Int, altIdx: Int) {
        parser = MessageParser(fields, dataHolder)
        this.tsIdx = tsIdx
        this.altIdx = altIdx
        streamFrame.setFields(fields)
    }
    fun tglStreamFrame() {
        streamFrame.isVisible = !streamFrame.isVisible
    }
    fun printNetStatistics() {
        logger.info(netStats.toString())
    }
    fun startPlotting(idx: Int) {
        val name = parser.fieldList[idx].fieldName
        if(openPlots.containsKey(idx)) {
            logger.info("Plot for $name is already open")
            return
        }
        val wrapper = ChartWrapper(name)
        openPlots[idx] = wrapper
        wrapper.isVisible = true
    }
}