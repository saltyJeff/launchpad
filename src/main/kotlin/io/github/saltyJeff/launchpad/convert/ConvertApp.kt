package io.github.saltyJeff.launchpad.convert

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import io.github.saltyJeff.launchpad.convert.Converter
import io.github.saltyJeff.launchpad.telem.Telemetry
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File

@CommandLine.Command(name = "convert", mixinStandardHelpOptions = true)
class ConvertApp: Runnable {
    @CommandLine.Option(names = ["-i", "--input"], paramLabel = "INPUT FILE", description = ["the input file for conversion"])
    var inputFile: File = File("rocket_data.bin")
    @CommandLine.Option(names = ["-c", "--config"], paramLabel = "CONFIG FILE", description = ["the config file for conversion"])
    var configFile: File = File("config.csv")
    @CommandLine.Option(names = ["-o", "--output"], paramLabel = "OUTPUT FILE", description = ["the output file for conversion"])
    var outputFile: File = File("rocket_data.csv")
    @CommandLine.Option(names = ["-v", "--verbose"], paramLabel = "verbose", description = ["Turn on verbose mode"])
    var verbose = false
    override fun run() {
        if(!verbose) {
            val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
            root.level = Level.WARN
        }
        Converter.beginConversion(this)
    }
}