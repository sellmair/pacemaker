package io.sellmair.broadheart.hrSensor

import kotlin.jvm.JvmInline

data class HrSensorInfo(
    val id: HrSensorId,
    val address: String,
    val vendor: Vendor,
    val rssi: Int? = null,
) {
    @JvmInline
    value class HrSensorId(val value: String)

    enum class Vendor {
        Polar, Garmin, Unknown
    }
}