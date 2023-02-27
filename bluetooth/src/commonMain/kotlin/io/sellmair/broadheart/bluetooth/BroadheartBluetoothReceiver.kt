package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.*
import kotlinx.coroutines.flow.SharedFlow
import kotlin.time.TimeMark

interface BroadheartBluetoothReceiver {
    val received: SharedFlow<ReceivedBroadheartPackage>
}

data class ReceivedBroadheartPackage(
    val receivedTime: TimeMark,
    val address: String,
    val userId: UserId,
    val userName: String,
    val sensorId: HeartRateSensorId,
    val heartRate: HeartRate,
    val heartRateLimit: HeartRate
)