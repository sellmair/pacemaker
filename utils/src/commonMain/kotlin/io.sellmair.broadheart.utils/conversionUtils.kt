package io.sellmair.broadheart.utils

import okio.Buffer

fun Int.encodeToByteArray(): ByteArray {
    val buffer = Buffer()
    buffer.writeInt(this)
    return buffer.readByteArray()
}

fun ByteArray.decodeToInt(): Int {
    val buffer = Buffer()
    buffer.write(this)
    return buffer.readInt()
}