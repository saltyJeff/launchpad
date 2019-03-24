package io.github.saltyJeff.launchpad.telem

import io.github.saltyJeff.launchpad.CField
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class StreamFrameTest {
    private val fields: List<CField> = listOf(
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("int8_t", "hello"),
        CField("uint8_t", "goodbye"),
        CField("uint32_t", "timestamp"),
        CField("double", "altitude")
    )
    private val dataHolder = MutableList<String>(fields.size) {"0"}
    val streamFrame = StreamFrame(dataHolder)
    init {
        streamFrame.setFields(fields)
        streamFrame.isVisible = true
    }

    @Test
    fun `Opens window`() {
        var lastTime = System.currentTimeMillis()
        var lastK = 0.0
        while(true) {
            if(lastTime + 5 < System.currentTimeMillis()) {
                dataHolder[fields.size - 2] = System.currentTimeMillis().toString()
                dataHolder[fields.size - 1] = (lastK).toString()
                lastK += 200
                lastTime = System.currentTimeMillis()
            }
        }
    }
}