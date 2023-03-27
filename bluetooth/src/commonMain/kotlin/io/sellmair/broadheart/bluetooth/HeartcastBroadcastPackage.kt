package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.UserId
import kotlin.time.TimeMark

data class HeartcastBroadcastPackage(
    val receivedTime: TimeMark,
    val deviceId: BleDeviceId,
    val userId: UserId,
    val userName: String,
    val sensorId: HeartRateSensorId,
    val heartRate: HeartRate,
    val heartRateLimit: HeartRate
)