package io.github.saltyJeff.launchpad

import io.github.saltyJeff.launchpad.build.BuildApp
import io.github.saltyJeff.launchpad.convert.ConvertApp
import io.github.saltyJeff.launchpad.telem.Kevin
import io.github.saltyJeff.launchpad.telem.TelemApp
import picocli.CommandLine

//this file declares all command line interfaces
@CommandLine.Command(name = "Launchpad", mixinStandardHelpOptions = true,
    subcommands = [
        BuildApp::class,
        TelemApp::class,
        ConvertApp::class,
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


fun main(args: Array<String>) {
    val cmd = CommandLine(LaunchpadApp())
    cmd.parseWithHandler(CommandLine.RunLast(), args)
}