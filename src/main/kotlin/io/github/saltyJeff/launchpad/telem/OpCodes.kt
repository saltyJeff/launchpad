package io.github.saltyJeff.launchpad.telem

enum class OpCodes {
    META_FIELDS,
    META_TYPES,
    META_MODULES,
    SET_MODULES_EN,
    GET_MODULES_EN,
    CALIBRATE,
    RESET,
    BENCH,
    SHUTDOWN,
    PING;

    companion object {
        fun fromByte(b: UByte): OpCodes {
            return OpCodes.values()[b.toInt() - 1]
        }
    }
    fun toByte(): UByte {
        return (this.ordinal + 1).toUByte()
    }
}