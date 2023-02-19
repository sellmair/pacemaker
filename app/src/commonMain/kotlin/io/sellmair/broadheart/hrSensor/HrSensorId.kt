package io.sellmair.broadheart.hrSensor

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class HrSensorId(val value: String)