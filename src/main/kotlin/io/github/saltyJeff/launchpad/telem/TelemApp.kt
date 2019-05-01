package io.github.saltyJeff.launchpad.telem

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.saltyJeff.launchpad.telem.Telemetry
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File

@CommandLine.Command(name = "telem", mixinStandardHelpOptions = true)
class TelemApp: Runnable {
    @CommandLine.Option(names = ["-s", "--serial"], paramLabel = "ARDUINO SERIAL FILE", description = ["the file representing the arduino"])
    var arduinoSerial: String = ""
    @CommandLine.Option(names = ["-R", "--no-radio"], paramLabel = "DISABLE RADIO", description = ["disables radio support"])
    var skipRadio = false
    @CommandLine.Option(names = ["-r", "--radio"], paramLabel = "RADIO SERIAL FILE", description = ["the file representing the radio"])
    var radioSerial: String = ""
    @CommandLine.Option(names = ["-c", "--config"], paramLabel = "CONFIG FILE", description = ["the file to save module settings to"])
    var settingsFile: File = File("config.csv")
    @CommandLine.Option(names = ["-o", "--output"], paramLabel = "OUTPUT FILE", description = ["the file to save the radio data to"])
    var outputFile: File = File("radio_output.bin")
    @CommandLine.Option(names = ["-v", "--verbose"], paramLabel = "verbose", description = ["Turn on verbose mode"])
    var verbose = false
    override fun run() {
        if(!verbose) {
            val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
            root.level = Level.WARN
        }
        Telemetry.beginTelem(this)
    }
}