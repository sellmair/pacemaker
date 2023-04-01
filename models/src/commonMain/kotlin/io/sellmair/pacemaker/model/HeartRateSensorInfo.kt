package io.sellmair.pacemaker.model

import kotlinx.serialization.Serializable

@Serializable
data class HeartRateSensorInfo(
    val id: HeartRateSensorId,
    val rssi: Int? = null,
)