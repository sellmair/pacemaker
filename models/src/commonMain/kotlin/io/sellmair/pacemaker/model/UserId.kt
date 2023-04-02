package io.sellmair.pacemaker.model

import kotlinx.serialization.Serializable
import okio.Buffer
import kotlin.jvm.JvmInline
import kotlin.random.Random

@Serializable
@JvmInline
value class UserId(val value: Long)

fun UserId.encodeToByteArray(): ByteArray {
    return Buffer().writeLong(value).readByteArray()
}

fun UserId(data: ByteArray): UserId? {
    val id = runCatching {
        Buffer().write(data).readLong()
    }.getOrNull() ?: return null
    return UserId(id)
}

fun randomUserId(): UserId {
    return UserId(Random.nextLong())
}