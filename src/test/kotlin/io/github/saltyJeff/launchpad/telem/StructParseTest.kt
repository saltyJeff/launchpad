package io.github.saltyJeff.launchpad.telem

import io.github.saltyJeff.launchpad.CField
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StructParseTest {
    private val fields: List<CField> = listOf(
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("uint16_t", "dank")
    )
    private val dataHolder = MutableList<String>(3) {""}
    private val parser: StructParser = StructParser(fields, dataHolder)

    private fun parseBytes(data: ByteArray): Boolean {
        return parser.parseBytes(data)
    }
    @Test
    fun `Should be able to do basic parses`() {
        Assertions.assertFalse(parseBytes(
            byteArrayOf(-1, -1, -1)
        ))
        Assertions.assertFalse(parseBytes(
            byteArrayOf(-1, 0x55, 0xA4.toByte()) //reverse order for little endian
        ))
        Assertions.assertTrue(parseBytes(
            byteArrayOf(0xA4.toByte(), 0x55)
        ))
        println(dataHolder)
        Assertions.assertEquals(dataHolder[0], "-1")
        Assertions.assertEquals(dataHolder[1], "255")
        Assertions.assertEquals(dataHolder[2], "42069")
        Assertions.assertFalse(parseBytes(
            byteArrayOf(-1, -1, 2)
        ))
        Assertions.assertFalse(parseBytes(
            byteArrayOf(4, 6, 8) //reverse order for little endian
        ))
        Assertions.assertTrue(parseBytes(
            byteArrayOf(0xA4.toByte(), 0x55)
        ))
        println(dataHolder)
    }
}