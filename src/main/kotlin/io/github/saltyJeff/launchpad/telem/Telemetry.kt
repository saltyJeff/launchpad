package io.github.saltyJeff.launchpad.telem

import com.digi.xbee.api.XBeeDevice
import com.fazecast.jSerialComm.SerialPort
import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.TYPE_DICT
import io.github.saltyJeff.launchpad.TelemApp
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.math.min

object Telemetry {
    val logger = LoggerFactory.getLogger("Telemetry manager")
    val fields: MutableList<CField> = mutableListOf()
    var moduleList = listOf<String>()
    lateinit var params: TelemApp
    var arduinoSerial: SerialPort? = null
    val input = Scanner(System.`in`)
    lateinit var job: Thread
    @Volatile var arduinoOut: Scanner? = null
    lateinit var radioOperator: RadioOperator

    val lineQueue = ConcurrentLinkedQueue<String>()
    @Volatile var queueLines = false
    //for some operations (like meta), the operation itself will
    //drain all lines, when this happens, set stopPrinting so we don't hang waiting for
    //a newline that will never come

    fun beginTelem(params: TelemApp) {
        this.params = params
        logger.info("Beginning telemetry")
        if(!params.settingsFile.exists()) {
            logger.warn("Settings file is empty")
        }
        else {
            val csvLines = params.settingsFile.readLines()
            loadSettings(csvLines[0], csvLines[1], csvLines[2])
        }
        if(!params.skipRadio) {
            logger.info("Initializing radio port")
            radioOperator = RadioOperator(selectPort(params.radioSerial).systemPortName)
        }
        job = thread(start = true) {
            while(true) {
                if(arduinoOut != null && arduinoOut!!.hasNextLine()) {
                    val nextLine = Telemetry.arduinoOut!!.nextLine()
                    if(queueLines) {
                        logger.debug("appended $nextLine to the queue")
                        lineQueue.add(nextLine)
                    }
                    println(nextLine)
                }
            }
        }
        while(true) {
            try {
                val next = input.nextLine()
                if(next.toLowerCase() == "quit") {
                    break
                }
                logger.debug("Recieved next command")
                interpretCmd(next)
                logger.debug("Ready for next command")
            }
            catch(e: Exception) {
                logger.error("Exception $e")
                e.printStackTrace()
            }
        }
        logger.warn("Quitting")
        input.close()
        job.interrupt()
        arduinoSerial?.closePort()
        Kevin.shutup()
        println("Goodbye")
        System.exit(0)
    }

    private fun interpretCmd(cmdStr: String) {
        //dump/plot is the only command that doesn't require an arduino serial port
        val cmd = cmdStr.toLowerCase()
        if(cmd == "help") {
            logger.info("Help Requested:")
            println("stream: Toggles the stream window")
            println("plot: Plots a specific field over time")
            println("begin: begins the serial communication to the arduino")
            println("meta: updates the metadata file for this build")
            println("enable: allows the user to turn off and on modules selectively")
            println("view: view which modules are active")
            println("calibrate: enters the arduino into calibration mode")
            println("reset: reboots the arduino")
            println("netstat: gets network statistics")
            println("ping: sends a ping")
            return
        }
        if(!params.skipRadio) {
            if(cmd == "stream") {
                radioOperator.tglStreamFrame()
                return
            }
            else if(cmd == "plot") {
                println("Select a field to plot")
                fields.withIndex().forEach {
                    println("${it.index}\t${it.value.fieldName}")
                }
                print("Select a field>")
                val idx = input.nextInt()
                input.nextLine()
                radioOperator.startPlotting(idx)
                return
            }
            else if(cmd == "netstat") {
                logger.info("Requested netstat")
                radioOperator.printNetStatistics()
            }
        }
        if(arduinoSerial == null || arduinoOut == null) {
            if(cmd != "begin") {
                logger.error("All other commands require the Arduino port to be started, please enter \"begin\" if that is your intention")
                return
            }
            logger.info("Preparing arduino serial port")
            arduinoSerial = selectPort(params.arduinoSerial)
            arduinoSerial!!.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0)
            arduinoSerial!!.baudRate = 115200
            arduinoSerial!!.openPort()
            while(!arduinoSerial!!.isOpen);
            arduinoOut = Scanner(arduinoSerial!!.inputStream)
            sendCmd(OpCodes.PING) //clear any lingering lines before proceeding
        }
        if(cmd == "meta") {
            logger.info("Pulling meta data from the device")
            queueLines = true
            lineQueue.clear()
            sendCmd(OpCodes.META_FIELDS)
            sendCmd(OpCodes.META_TYPES)
            sendCmd(OpCodes.META_MODULES)

            //block until we get our 3 lines
            while(lineQueue.size < 3);
            loadSettings(lineQueue.poll(), lineQueue.poll(), lineQueue.poll())
            writeSettings()
            queueLines = false
        }
        else if(cmd == "enable") {
            println("The following is a list of modules. Enter a binary string with 1 meaning on and 0 meaning off")
            println(moduleList)
            print("Binary enable flags>")
            val cmdStr = input.nextLine()
                .take(8)
                .padEnd(8, '0')
            val cmd = cmdStr
                .toUByte(2)
            logger.info("Setting enable flags to $cmdStr")
            sendCmd(OpCodes.SET_MODULES_EN, cmd)
        }
        else if(cmd == "view") {
            println("The following is a list of modules. A binary 1 in its position from left to right means it is enabled")
            println(moduleList)
            sendCmd(OpCodes.GET_MODULES_EN)
        }
        else if(cmd == "calibrate") {
            logger.info("Entering callibration mode")
            sendCmd(OpCodes.CALIBRATE)
        }
        else if(cmd == "reset") {
            logger.info("Restarting machine")
            sendCmd(OpCodes.RESET)
        }
        else if(cmd == "bench") {
            logger.info("Requested benchmark")
            sendCmd(OpCodes.BENCH)
        }
        else if(cmd == "ping") {
            logger.info("Requested ping")
            sendCmd(OpCodes.PING)
        }
        else if(cmd == "shutdown") {
            logger.warn("Requesting shutdown")
            sendCmd(OpCodes.SHUTDOWN)
        }
        else if(cmd != "begin") {
            logger.error("Command $cmd is not understood, type 'help' for help")
        }
    }
    private val writeBuffer = ByteArray(3)
    private fun sendCmd(op: OpCodes, vararg operands: UByte) {
        writeBuffer[0] = op.toByte().toByte()
        if(operands.size > 2) {
            logger.error("Recieved an invalid command with more than 2 bytes for operands, ignoring")
        }
        var i = 0
        while(i < min(2, operands.size)) {
            writeBuffer[i+1] = operands[i].toByte()
            i++
        }
        arduinoSerial!!.writeBytes(writeBuffer, min(3, 1 + operands.size).toLong())
        logger.debug("Sent data package: ${writeBuffer.copyOfRange(0, 1 + operands.size).map{ it.toUByte() }}")
    }
    private fun selectPort(desc: String): SerialPort {
        if(desc == "") {
            logger.warn("No serial port specified, please select from below")
            val ports = SerialPort.getCommPorts()
            ports.withIndex().forEach{
                println("${it.index}:\t${it.value.descriptivePortName}")
            }
            print("Select a port number>")
            val idx = input.nextInt()
            input.nextLine()
            return ports[idx]
        }
        else {
            return SerialPort.getCommPort(desc)
        }
    }
    var timestampIdx = -1
    var altitudeIdx = -1
    private fun loadSettings(line1: String, line2: String, line3: String) {
        fields.clear()
        val nameSplit = line1.split(',')
        val typeSplit = line2.split(',')
        val moduleSplit = line3.split(',')
        if(nameSplit.size != typeSplit.size) {
            throw Exception("CSV lines have different token counts")
        }
        nameSplit.indices.forEach {
            if(nameSplit[it] == "timestamp") {
                timestampIdx = it
            }
            if(nameSplit[it].contains("altitude")) {
                altitudeIdx = it
            }
            fields.add(CField(typeSplit[it], nameSplit[it]))
        }
        if(!params.skipRadio) {
            radioOperator.fieldsChanged()
        }
        logger.info("Meta info about the chip:\nField Names: $line1\nField Types: $line2\nModules: $line3")
        moduleList = moduleSplit
    }
    private fun writeSettings() {
        logger.warn("Writing configuration file")
        val nameBuilder = StringBuilder()
        val typeBuilder = StringBuilder()
        fields.forEach {
            nameBuilder.append("${it.fieldName},")
            typeBuilder.append("${it.typeName},")
        }
        //remove trailing comma
        val nameCsv = nameBuilder.toString().dropLast(1)
        val typeCsv = typeBuilder.toString().dropLast(1)
        params.settingsFile.setWritable(true)
        params.settingsFile.writeText("$nameCsv\n$typeCsv\n${moduleList.joinToString()}")
    }
}