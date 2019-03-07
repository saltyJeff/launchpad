package io.github.saltyJeff.launchpad.telem

import io.github.saltyJeff.launchpad.CField
import io.github.saltyJeff.launchpad.TYPE_DICT
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

val startDelim = 0xFF.toUByte()
val end1Delim = 0xA4.toUByte()
val end2Delim = 0x55.toUByte()

class StructParser(private val fieldList: List<CField>, private val dataHolder: MutableList<String>) {
    private var writeIdx = 0
    private val pkgSize: Int
    private val buffer: ByteArray
    init {
        //resize dataHolder
        dataHolder.clear()
        dataHolder.addAll(Collections.nCopies(fieldList.size, "not initialized"))
        //resize bytebuffer
        var pkgSize = 0
        fieldList.forEach {
            pkgSize += TYPE_DICT[it.typeName]!!
        }
        buffer = ByteArray((pkgSize + 4) * 2)
        this.pkgSize = pkgSize
    }

    //make functional for unit testing
    fun parseBytes(data: ByteArray): Boolean {
        var needUpdate = false
        for(b in data) {
            buffer[writeIdx] = b
            writeIdx++
            if(b.toUByte() == end2Delim) {
                needUpdate = needUpdate || scanStructs()
            }
        }
        return needUpdate
    }
    private fun scanStructs(): Boolean {
        var i = 1
        while(i < writeIdx) {
            if(buffer[i].toUByte() == startDelim && buffer[i-1].toUByte() == startDelim) {
                //jump head to new index
                val bufStart = i + 1
                i = bufStart + 1 + pkgSize
                if(i < buffer.size &&
                    buffer[i].toUByte() == end2Delim &&
                    buffer[i-1].toUByte() == end1Delim) {

                    val inputSlice = buffer.slice(bufStart..i-2).toByteArray()
                    val inputBuffer = ByteBuffer.wrap(inputSlice).order(ByteOrder.LITTLE_ENDIAN)
                    fieldList.withIndex().forEach {
                        val idx = it.index
                        val typeName = it.value.typeName
                        when(typeName) {
                            "int8_t" ->  dataHolder[idx] = inputBuffer.get().toString()
                            "uint8_t" -> dataHolder[idx] = inputBuffer.get().toUByte().toString()
                            "int16_t" -> dataHolder[idx] = inputBuffer.short.toString()
                            "uint16_t" -> dataHolder[idx] = inputBuffer.short.toUShort().toString()
                            "int32_t" -> dataHolder[idx] = inputBuffer.int.toString()
                            "uint32_t" -> dataHolder[idx] = inputBuffer.int.toUInt().toString()
                            "int64_t" -> dataHolder[idx] = inputBuffer.long.toString()
                            "uint64_t" -> dataHolder[idx] = inputBuffer.long.toULong().toString()
                            "float" -> dataHolder[idx] = inputBuffer.float.toString()
                            "double" -> dataHolder[idx] = inputBuffer.double.toString()
                        }
                    }

                    val numCopy = buffer.size - i - 1
                    val startGood = i + 1
                    for(j in startGood.until(startGood + numCopy)) {
                        buffer[j - startGood] = buffer[j]
                    }
                    for(k in numCopy.until(buffer.size)) {
                        buffer[k] = 0
                    }
                    writeIdx -= startGood
                    return true
                }
            }
            i++
        }
        return false
    }
}