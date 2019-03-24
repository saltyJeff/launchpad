package io.github.saltyJeff.launchpad.convert

import io.github.saltyJeff.launchpad.CField
import java.io.File
import java.util.*

class ConfigData(file: File) {
    val fields: List<CField>
    val modules: List<String>
    init {
        val reader = Scanner(file);
        val fieldNameLine = reader.nextLine()
        val fieldTypeLine = reader.nextLine()
        val moduleLine = reader.nextLine()
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
}