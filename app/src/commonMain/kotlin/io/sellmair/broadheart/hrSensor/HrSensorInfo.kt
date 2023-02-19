package io.sellmair.broadheart.hrSensor

import kotlinx.serialization.Serializable

@Serializable
data class HrSensorInfo(
    val id: HrSensorId,
    val address: String,
    val vendor: Vendor,
    val rssi: Int? = null,
) {

    enum class Vendor {
        Polar, Garmin, Unknown
    }
}