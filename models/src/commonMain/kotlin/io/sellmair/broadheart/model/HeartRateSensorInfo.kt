package io.sellmair.broadheart.model

import kotlinx.serialization.Serializable

@Serializable
data class HeartRateSensorInfo(
    val id: HeartRateSensorId,
    val address: String?,
    val vendor: Vendor,
    val rssi: Int? = null,
) {
    enum class Vendor {
        Polar, Garmin, Unknown
    }
}