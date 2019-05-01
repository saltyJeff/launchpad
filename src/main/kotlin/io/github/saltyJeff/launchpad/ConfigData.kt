package io.github.saltyJeff.launchpad

import java.io.File
import java.util.*

/**
 * Reads and writes configuration CSVs that cache the specific parameters of a specific chip
 * @property fields The list of fields in the CSV
 * @property modules The list of modules on the chip
 * @property altIdx the index of the field that stores the altimeter, or -1 if it doesn't exist
 * @property tsIdx the index of the field that stores the timestamp, or -1 if it doesn't exist
 */
class ConfigData(file: File) {
    lateinit var fields: List<CField>
    lateinit var modules: List<String>
    // index of the alitimeter field
    var altIdx = -1
    //index of the timestamp field
    var tsIdx = -1
    init {
        updateConfig(file)
    }

    /**
     * Updates a config file from a stored CSV file
     * @param newFile the file to read from
     */
    fun updateConfig(newFile: File) {
        val reader = Scanner(newFile)
        val fieldNameLine = reader.nextLine()
        val fieldTypeLine = reader.nextLine()
        val moduleLine = reader.nextLine()
        updateConfig(fieldNameLine, fieldTypeLine, moduleLine)
    }
    /**
     * Updates a config file from the lines of a CSV file
     * @param fieldNameLine the line containing the names of the fields
     * @param fieldTypeLine the line containing the types of the fields
     * @param moduleLine the line containing the names of the modules
     */
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

        //update field indicies of interest
        altIdx = -1
        tsIdx = -1
        fields.withIndex().forEach {
            val fieldName = it.value.fieldName.toLowerCase()
            if(fieldName.contains("timestamp")) {
                tsIdx = it.index
            }
            else if(fieldName.contains("altitude")) {
                altIdx = it.index
            }
        }
    }

    /**
     * Writes the configuration to a CSV file
     * @param newFile the file to write to
     */
    fun writeToFile(newFile: File) {
        newFile.createNewFile()
        newFile.setWritable(true)
        val nameCsv = fields.csvJoin { it.fieldName }
        val typeCsv = fields.csvJoin { it.typeName }
        val moduleCsv = modules.csvJoin()
        newFile.writeText("$nameCsv\n$typeCsv\n$moduleCsv")
    }
    /**
     * Check if there is an altimeter stored
     * @return true if altimeter does exist
     */
    fun hasAltimeter(): Boolean {
        return altIdx != -1
    }
    fun hasTimestamp(): Boolean {
        return tsIdx != -1
    }
}