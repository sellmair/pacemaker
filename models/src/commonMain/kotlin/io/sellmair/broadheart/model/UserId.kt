package io.sellmair.broadheart.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.random.Random

@Serializable
@JvmInline
value class UserId(val value: Long)

fun randomUserId(): UserId {
    return UserId(Random.nextLong())
}