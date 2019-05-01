package io.github.saltyJeff.launchpad.telem

import com.fazecast.jSerialComm.SerialPort
import io.github.saltyJeff.launchpad.ConfigData
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.min

object Telemetry {
    val logger = LoggerFactory.getLogger("Telemetry manager")
    lateinit var config: ConfigData
    lateinit var params: TelemApp
    var arduinoSerial: SerialPort? = null
    val input = Scanner(System.`in`)
    lateinit var job: Thread
    @Volatile var arduinoOut: Scanner? = null
    var radioOperator: RadioOperator? = null
    var termMode = false

    val lineQueue = ConcurrentLinkedQueue<String>()
    @Volatile var queueLines = false
    //for some operations (like meta), the operation itself will
    //drain all lines, when this happens, set stopPrinting so we don't hang waiting for
    //a newline that will never come

    fun beginTelem(params: TelemApp) {
        this.params = params
        logger.warn("Beginning telemetry")

        if(!params.settingsFile.exists()) {
            logger.warn("Settings file is empty")
        }
        else {
            config = ConfigData(params.settingsFile)
        }

        if(!params.skipRadio) {
            logger.info("Initializing radio port")
            radioOperator = RadioOperator(
                selectPort(params.radioSerial).systemPortName)
        }
        if(params.arduinoSerial != "") {
            beginSerial()
        }

        job = thread(start = true) { //thread continually polls for newlines and dumps it out
            while(true) {
                if(arduinoOut != null && arduinoOut!!.hasNextLine()) {
                    val nextLine = arduinoOut!!.nextLine()
                    if(queueLines) { //boolean that the next few lines are "important"
                        logger.debug("appended $nextLine to the queue")
                        //store so the other queue can use it
                        lineQueue.add(nextLine)
                    }
                    println(nextLine)
                }
            }
        }
        //begin repl
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
        if(arduinoSerial != null) {
            sendCmd(OpCodes.SHUTDOWN)
        }
        runBlocking {
            delay(1500)
        }
        input.close()
        job.interrupt()
        arduinoSerial?.closePort()
        radioOperator?.close()
        Kevin.shutup()
        println("Goodbye")
        System.exit(0)
    }

    private fun interpretCmd(cmdStr: String) {
        val cmd = cmdStr.toLowerCase()
        if(cmd == "help") {
            logger.info("Help Requested:")
            dumpHelp()
            return
        }
        //help stuff
        if(!params.skipRadio && radioOperator != null) {
            val radioOp = radioOperator!!
            if(cmd == "stream") {
                radioOp.tglStreamFrame()
                return
            }
            else if(cmd == "plot") {
                println("Select a field to plot")
                config.fields.withIndex().forEach {
                    println("${it.index}\t${it.value.fieldName}")
                }
                print("Select a field>")
                val idx = input.nextInt()
                input.nextLine()
                radioOp.startPlotting(idx)
                return
            }
            else if(cmd == "netstat") {
                logger.info("Requested netstat")
                radioOp.printNetStatistics()
                return
            }
        }
        //the radio hasn't been set up and you're not using a radio command, so now pick a arduino radio
        if(radioNotInit()) {
            if(cmd != "begin") {
                logger.error("All other commands require the Arduino port to be started, please enter \"begin\" if that is your intention")
                return
            }
            beginSerial() //clear any lingering lines before proceeding
        }
        if(termMode) {
            if(cmdStr == "exitterm") {
                logger.info("Exiting terminal mode")
                termMode = false
                return
            }
            val fullCmd = cmdStr+"\n"
            arduinoSerial!!.writeBytes(fullCmd.toByteArray(), fullCmd.length.toLong())
            return
        }
        //giant if of DEATH (mayb create a bunch of private funs or something but whatever)
        //then again most of these are single lines so whatevs
        if(cmd == "meta") {
            logger.info("Pulling meta data from the device")
            queueLines = true
            lineQueue.clear()
            sendCmd(OpCodes.META_FIELDS)
            sendCmd(OpCodes.META_TYPES)
            sendCmd(OpCodes.META_MODULES)

            //block until we get our 3 lines
            while(lineQueue.size < 3);
            config.updateConfig(lineQueue.poll(), lineQueue.poll(), lineQueue.poll())
            config.writeToFile(params.settingsFile)
            queueLines = false
        }
        else if(cmd == "enable") {
            println(config.modules)
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
            println(config.modules)
            sendCmd(OpCodes.GET_MODULES_EN)
        }
        else if(cmd == "calibrate") {
            println("Enter the index of the module to calibrate, or 8 to calibrate all of them")
            print("Calibration index>")
            val calibNum = input.nextInt()
            input.nextLine()
            sendCmd(OpCodes.CALIBRATE, calibNum.toUByte())
            enterTerm()
        }
        else if(cmd == "reset") {
            logger.info("Restarting machine")
            sendCmd(OpCodes.RESET)
        }
        else if(cmd == "bench") {
            logger.info("Requested benchmark")
            println(config.modules)
            sendCmd(OpCodes.BENCH)
        }
        else if(cmd == "ping") {
            logger.info("Requested ping")
            sendCmd(OpCodes.PING)
        }
        else if(cmd == "term") {
            enterTerm()
        }
        else if(cmd != "begin") {
            logger.error("Command $cmd is not understood, type 'help' for help")
        }
    }
    fun radioNotInit(): Boolean {
        return arduinoSerial == null || arduinoOut == null
    }
    fun beginSerial() {
        logger.info("Preparing arduino serial port")
        val arduinoSerial = selectPort(params.arduinoSerial)
        arduinoSerial.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0)
        arduinoSerial.baudRate = 115200
        arduinoSerial.openPort()
        while(!arduinoSerial.isOpen); //hopefully this boi doesn't lock into oblivion
        arduinoOut = Scanner(arduinoSerial.inputStream)
        this.arduinoSerial = arduinoSerial
        sendCmd(OpCodes.PING)
    }
    fun dumpHelp() {
        println("stream: Toggles the stream window")
        println("plot: Plots a specific field over time")
        println("begin: begins the serial communication to the arduino")
        println("meta: updates the metadata file for this build")
        println("enable: allows the user to turn off and on modules selectively")
        println("view: view which modules are active")
        println("calibrate: enters the arduino into calibration mode")
        println("reset: reboots the arduino")
        println("netstat: gets network statistics")
        println("term: enters terminal mode")
        println("ping: sends a ping")
    }
    fun enterTerm() {
        logger.info("Entering terminal mode. Any commands will be instantly sent to the Arduino. Type exitterm to exit")
        termMode = true
    }
    private val writeBuffer = ByteArray(3)
    private fun sendCmd(op: OpCodes, vararg operands: UByte) {
        writeBuffer[0] = op.toByte().toByte()
        if(operands.size > 2) {
            logger.error("Received an invalid command with more than 2 bytes for operands, ignoring")
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
}