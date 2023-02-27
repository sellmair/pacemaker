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

fun Long.encodeToByteArray(): ByteArray {
    val buffer = Buffer()
    buffer.writeLong(this)
    return buffer.readByteArray()
}

fun ByteArray.decodeToLong(): Long {
    val buffer = Buffer()
    buffer.write(this)
    return buffer.readLong()
}