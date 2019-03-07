package io.github.saltyJeff.launchpad

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.sun.speech.freetts.Gender
import com.sun.speech.freetts.VoiceManager
import io.github.saltyJeff.launchpad.build.RocketBuild
import io.github.saltyJeff.launchpad.build.defaultSketchbook
import io.github.saltyJeff.launchpad.telem.Kevin
import io.github.saltyJeff.launchpad.telem.Telemetry
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.io.File

//this file declares all command line interfaces
@CommandLine.Command(name = "Launchpad", mixinStandardHelpOptions = true,
    subcommands = [
        BuildApp::class,
        TelemApp::class,
        CommandLine.HelpCommand::class
    ],
    version = ["UCLA Rocket Project", "2019.2", "Run launchpad help for a list of commands"]
)
class LaunchpadApp: Runnable {
    override fun run() {
        Kevin.speak(utterances.random())
        println(rocket)
        CommandLine(this).printVersionHelp(System.out)
        Kevin.finishUp()
    }
}
@CommandLine.Command(name = "build", mixinStandardHelpOptions = true)
class BuildApp: Runnable {
    @CommandLine.Option(names = ["-P", "--no-pack"], paramLabel = "disables struct packing", description = ["turn off struct packing"])
    var noPack = false
    @CommandLine.Option(names = ["-W", "--no-watch"], paramLabel = "disables watch mode", description = ["turn off watch mode"])
    var noWatch = false
    @CommandLine.Option(names = ["-d", "--dir"], paramLabel = "SOURCE DIR", description = ["the folder containing the source code"])
    lateinit var projectDir: File
    @CommandLine.Option(names = ["-o", "--output"], paramLabel = "OUTPUT DIR", description = ["the folder to output"])
    lateinit var outputDir: File
    @CommandLine.Option(names = ["-l", "--libs"], paramLabel = "LIBRARIES DIR", description = ["the folder to store libraries"])
    var libDir: File = defaultSketchbook()
    @CommandLine.Option(names = ["-v", "--verbose"], paramLabel = "verbose", description = ["Turn on verbose mode"])
    var verbose = false
    var watch = true
    override fun run() {
        if(!verbose) {
            val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
            root.level = Level.INFO
        }
        watch = !noWatch
        RocketBuild.rocketBuild(this)
    }
}
@CommandLine.Command(name = "telem", mixinStandardHelpOptions = true)
class TelemApp: Runnable {
    @CommandLine.Option(names = ["-s", "--serial"], paramLabel = "ARDUINO SERIAL FILE", description = ["the file representing the arduino"])
    var arduinoSerial: String = ""
    @CommandLine.Option(names = ["-R", "--no-radio"], paramLabel = "DISABLES RADIO", description = ["disables radio support"])
    var skipRadio = false
    @CommandLine.Option(names = ["-r", "--radio"], paramLabel = "RADIO SERIAL FILE", description = ["the file representing the radio"])
    var radioSerial: String = ""
    @CommandLine.Option(names = ["-c", "--config"], paramLabel = "CONFIG FILE", description = ["the file to save module settings to"])
    var settingsFile: File = File("config.csv")
    @CommandLine.Option(names = ["-o", "--output"], paramLabel = "OUTPUT FILE", description = ["the file to save the radio data to"])
    var outputFile: File = File("output.bin")
    @CommandLine.Option(names = ["-v", "--verbose"], paramLabel = "verbose", description = ["Turn on verbose mode"])
    var verbose = false
    override fun run() {
        if(!verbose) {
            val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
            root.level = Level.INFO
        }
        Telemetry.beginTelem(this)
    }
}
fun main(args: Array<String>) {
    val cmd = CommandLine(LaunchpadApp())
    cmd.parseWithHandler(CommandLine.RunLast(), args)
}