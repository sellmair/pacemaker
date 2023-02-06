package io.sellmair.broadheart

import kotlin.jvm.JvmInline
import kotlin.random.Random

@JvmInline
value class UserId(val value: Long)

fun randomUID(): UserId {
    return UserId(Random.nextLong())
}
