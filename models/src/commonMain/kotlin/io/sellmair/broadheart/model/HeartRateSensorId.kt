package io.sellmair.broadheart.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class HeartRateSensorId(val value: String)