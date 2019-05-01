package io.github.saltyJeff.launchpad.convert

import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.csvJoin
import java.io.File
import java.util.*

class ConfigData(file: File) {
    lateinit var fields: List<CField>
    lateinit var modules: List<String>
    init {
        updateConfig(file)
    }
    fun updateConfig(newFile: File) {
        val reader = Scanner(newFile)
        val fieldNameLine = reader.nextLine()
        val fieldTypeLine = reader.nextLine()
        val moduleLine = reader.nextLine()
        updateConfig(fieldNameLine, fieldTypeLine, moduleLine)
    }
    fun updateConfig(fieldNameLine: String, fieldTypeLine: String, moduleLine: String) {
        modules = moduleLine.split(',')
        val fieldNameList = fieldNameLine.split(',')
        val fieldTypeList = fieldTypeLine.split(',')
        if(fieldNameList.size != fieldTypeList.size) {
            throw Exception("Field lengths don't match")
        }
        val fieldList = mutableListOf<CField>()
        for(i in fieldNameList.indices) {
            fieldList.add(CField(fieldTypeList[i], fieldNameList[i]))
        }
        fields = fieldList
    }
    fun writeToFile(newFile: File) {
        newFile.createNewFile()
        newFile.setWritable(true)
        val nameCsv = fields.csvJoin { it.fieldName }
        val typeCsv = fields.csvJoin { it.typeName }
        val moduleCsv = modules.csvJoin()
        newFile.writeText("$nameCsv\n$typeCsv\n$moduleCsv")
    }
}