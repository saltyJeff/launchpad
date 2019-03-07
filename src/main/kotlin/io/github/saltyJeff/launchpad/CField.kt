package io.github.saltyJeff.launchpad

class CField(val typeName: String, val fieldName: String) {
    override fun toString(): String {
        return "$fieldName=$typeName"
    }
}

fun typeKnown(typeName: String): Boolean {
    return TYPE_DICT.containsKey(typeName)
}
val TYPE_DICT = hashMapOf(
    "int8_t" to 1,
    "uint8_t" to 1,
    "int16_t" to 2,
    "uint16_t" to 2,
    "int32_t" to 4,
    "uint32_t" to 4,
    "int64_t" to 8,
    "uint64_t" to 8,
    "float" to 4,
    "double" to 8
)