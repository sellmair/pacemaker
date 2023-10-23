package io.sellmair.pacemaker.model

data class HeartRateSensorInfo(
    val id: HeartRateSensorId,
    val rssi: Int? = null,
)