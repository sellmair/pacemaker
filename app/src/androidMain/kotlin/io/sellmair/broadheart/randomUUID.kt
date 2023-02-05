package io.sellmair.broadheart

actual fun randomUUID(): UUID {
    return UUID(java.util.UUID.randomUUID().toString())
}
