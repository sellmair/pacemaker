package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.UserId
import kotlin.time.TimeMark

data class PacemakerBroadcastPackage(
    val receivedTime: TimeMark,
    val deviceId: BleDeviceId,
    val userId: UserId,
    val userName: String,
    val sensorId: HeartRateSensorId,
    val heartRate: HeartRate,
    val heartRateLimit: HeartRate
)