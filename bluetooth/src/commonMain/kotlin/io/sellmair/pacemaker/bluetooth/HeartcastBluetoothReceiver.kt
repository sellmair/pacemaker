package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.UserId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.time.TimeSource


fun BleConnection.receivePacemakerBroadcastPackages(): Flow<PacemakerBroadcastPackage> = channelFlow {
    var userId: UserId? = null
    var sensorId: HeartRateSensorId? = null
    var userName: String? = null
    var heartRate: HeartRate? = null
    var heartRateLimit: HeartRate? = null

    suspend fun emitIfPossible() {
        send(
            PacemakerBroadcastPackage(
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
            getValue(PacemakerServiceDescriptors.userIdCharacteristic).collect {
                userId = runCatching { UserId(it) }.getOrNull()
                if (userId == null) println("Failed decoding userId")
                emitIfPossible()
            }
        }

        launch {
            getValue(PacemakerServiceDescriptors.sensorIdCharacteristic).collect {
                sensorId = runCatching { HeartRateSensorId(it.decodeToString()) }.getOrNull()
                if (sensorId == null) println("Failed decoding sensorId")
                emitIfPossible()
            }
        }

        launch {
            getValue(PacemakerServiceDescriptors.userNameCharacteristic).collect {
                userName = runCatching { it.decodeToString() }.getOrNull()
                if (userName == null) println("Failed decoding userName")
                emitIfPossible()
            }
        }

        launch {
            getValue(PacemakerServiceDescriptors.heartRateCharacteristic).collect {
                heartRate = HeartRate(it)
                if (heartRate == null) println("Failed decoding heartRate")
                emitIfPossible()
            }
        }

        launch {
            getValue(PacemakerServiceDescriptors.heartRateLimitCharacteristic).collect {
                heartRateLimit = HeartRate(it)
                if (heartRateLimit == null) println("Failed decoding heartRateLimit")
                emitIfPossible()
            }
        }
    }
}