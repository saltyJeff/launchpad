package io.github.saltyJeff.launchpad.telem

import com.digi.xbee.api.XBeeDevice
import com.digi.xbee.api.models.XBeeMessage
import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.MessageParser
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class RadioOperator(port: String): Closeable {
    private val device = XBeeDevice(port, 9600)
    private val dataHolder = mutableListOf<String>()
    private lateinit var parser: MessageParser
    private val logger = LoggerFactory.getLogger("Radio Operator")

    //network statistics
    private val netStats = NetworkStatistics()

    //gui holders
    private val openPlots = mutableMapOf<Int, ChartWrapper>()
    var streamFrame = StreamFrame(dataHolder)

    private var lastK = 0
    private val writePath: Path
    init {
        println("Opening")
        device.open()
        println(device.operatingMode.toString())
        logger.warn("Opened serial port $port")
        if(!Telemetry.params.outputFile.exists()) {
            Telemetry.params.outputFile.createNewFile()
        }
        logger.warn("Timestamp index: ${Telemetry.config.tsIdx}, Altitude index: ${Telemetry.config.altIdx}")
        writePath = Telemetry.params.outputFile.toPath()
        device.addDataListener {onRadioData(it)}
    }
    fun onRadioData(msg: XBeeMessage) {
        Files.write(writePath, msg.data, StandardOpenOption.APPEND)
        parser.parseBytes(msg.data) {
            val thisTime = dataHolder[Telemetry.config.altIdx].toLong()
            netStats.msgReceived(thisTime)

            openPlots.entries.forEach {(i, frame) ->
                if(frame.closed) {
                    openPlots.remove(i)
                    return@forEach
                }
                frame.addData(thisTime / 1000.0, dataHolder[i].toDouble())
            }
            if(Telemetry.config.hasAltimeter()) {
                val thisHeight = (dataHolder[Telemetry.config.altIdx].toDouble()  / 1000).toInt()
                if(thisHeight > lastK && lastK > 0) {
                    lastK = thisHeight
                    Kevin.speak("$thisHeight meters")
                }
            }
        }
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
    override fun close() {
        device.close()
    }
}