package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.UserId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import okio.Buffer
import kotlin.time.TimeSource


fun BleConnection.receiveHeartcastBroadcastPackages(): Flow<HeartcastBroadcastPackage> = channelFlow {
    var userId: UserId? = null
    var sensorId: HeartRateSensorId? = null
    var userName: String? = null
    var heartRate: HeartRate? = null
    var heartRateLimit: HeartRate? = null

    suspend fun emitIfPossible() {
        send(
            HeartcastBroadcastPackage(
                receivedTime = TimeSource.Monotonic.markNow(),
                deviceId = id,
                userId = userId ?: UserId(0),
                sensorId = sensorId ?: return,
                userName = userName ?: "n/a",
                heartRate = heartRate ?: return,
                heartRateLimit = heartRateLimit ?: return
            )
        )
    }

    coroutineScope {
        launch {
            getValue(HeartcastBleService.userIdCharacteristic).collect {
                userId = runCatching { UserId(Buffer().write(it).readLong()) }.getOrNull()
                if (userId == null) println("Failed decoding userId")
                emitIfPossible()
            }
        }

        launch {
            getValue(HeartcastBleService.sensorIdCharacteristic).collect {
                sensorId = runCatching { HeartRateSensorId(it.decodeToString()) }.getOrNull()
                if (sensorId == null) println("Failed decoding sensorId")
                emitIfPossible()
            }
        }

        launch {
            getValue(HeartcastBleService.userNameCharacteristic).collect {
                userName = runCatching { it.decodeToString() }.getOrNull()
                if (userName == null) println("Failed decoding userName")
                emitIfPossible()
            }
        }

        launch {
            getValue(HeartcastBleService.heartRateCharacteristic).collect {
                heartRate = runCatching { HeartRate(Buffer().write(it).readInt()) }.getOrNull()
                if (heartRate == null) println("Failed decoding heartRate")
                emitIfPossible()
            }
        }

        launch {
            getValue(HeartcastBleService.heartRateLimitCharacteristic).collect {
                heartRateLimit = runCatching { HeartRate(Buffer().write(it).readInt()) }.getOrNull()
                if (heartRateLimit == null) println("Failed decoding heartRateLimit")
                emitIfPossible()
            }
        }
    }
}