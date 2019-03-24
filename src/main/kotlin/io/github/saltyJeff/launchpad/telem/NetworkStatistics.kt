package io.github.saltyJeff.launchpad.telem


class NetworkStatistics {
    private var startTime: Long = 0
    private var lastTime: Long = 0
    private var thisTime: Long = 0
    private var msgsRead: Long = 0
    private var initialized: Boolean = false

    fun msgReceived(timestamp: Long) {
        msgsRead++
        if(!initialized) {
            //can't find delta between infinity past to now
            startTime = timestamp
            lastTime = timestamp
            thisTime = timestamp
            initialized = true
            return
        }
        //slide up a level
        lastTime = thisTime
        thisTime = timestamp
    }

    override fun toString(): String {
        //total # of messages over total time elapsed
        val totalHz = msgsRead / ((thisTime - startTime) / 1000.0)
        //1 message over how much time it took between the last msg
        val thisHz = 1 / ((thisTime - lastTime) / 1000.0)
        return """
            Network statistics @ ${"%.2f".format(thisTime / 1000.0)}
            ($thisHz/$totalHz)
        """.trimIndent()
    }
}