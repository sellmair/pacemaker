package io.sellmair.broadheart

@JvmInline
value class UUID(val value: String)

expect fun randomUUID(): UUID
