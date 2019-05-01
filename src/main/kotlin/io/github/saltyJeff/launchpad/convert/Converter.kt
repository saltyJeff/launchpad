package io.github.saltyJeff.launchpad.convert

import io.github.saltyJeff.launchpad.ConfigData
import io.github.saltyJeff.launchpad.MessageParser
import io.github.saltyJeff.launchpad.csvJoin
import io.github.saltyJeff.launchpad.telem.NetworkStatistics
import java.io.*

object Converter {
    fun beginConversion(params: ConvertApp) {
        val byteBuffer = ByteArray(512)
        val reader = RandomAccessFile(params.inputFile, "r")
        val output = PrintWriter(BufferedWriter(FileWriter(params.outputFile)))
        val config = ConfigData(params.configFile)
        val dataHolder = mutableListOf<String>()
        val parser = MessageParser(config.fields, dataHolder)
        var tsIdx = 0
        for(i in config.fields.indices) {
            if(config.fields[i].fieldName == "timestamp") {
                tsIdx = i
            }
        }
        val netStats = NetworkStatistics()
        output.println(config.fields.csvJoin { it.fieldName })
        while(reader.read(byteBuffer) == 512) {
            parser.parseBytes(byteBuffer) {
                output.println(dataHolder.joinToString())
                netStats.msgReceived(dataHolder[tsIdx].toLong())
            }
        }
        output.close()
        println(netStats)
    }
}