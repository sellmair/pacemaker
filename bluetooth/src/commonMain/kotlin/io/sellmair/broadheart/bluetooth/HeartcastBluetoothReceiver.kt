@file:Suppress("OPT_IN_USAGE")

package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.UserId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okio.Buffer
import kotlin.time.TimeSource

suspend fun Ble.receiveHeartcastBroadcastPackages(): Flow<HeartcastBroadcastPackage> {
    return startClient(HeartcastBleService.service).peripherals
        .map { it.connect() }
        .flatMapMerge { connection ->
            val peripheralId = connection.peripheralId
            var userId: UserId? = null
            var sensorId: HeartRateSensorId? = null
            var userName: String? = null
            var heartRate: HeartRate? = null
            var heartRateLimit: HeartRate? = null

            channelFlow {
                suspend fun emitIfPossible() {
                    send(
                        HeartcastBroadcastPackage(
                            receivedTime = TimeSource.Monotonic.markNow(),
                            deviceId = peripheralId,
                            userId = userId ?: return,
                            sensorId = sensorId ?: return,
                            userName = userName ?: return,
                            heartRate = heartRate ?: return,
                            heartRateLimit = heartRateLimit ?: return
                        )
                    )
                }

                coroutineScope {
                    launch {
                        connection.getValue(HeartcastBleService.userIdCharacteristic).collect {
                            userId = runCatching { UserId(Buffer().write(it).readLong()) }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.getValue(HeartcastBleService.sensorIdCharacteristic).collect {
                            sensorId = runCatching { HeartRateSensorId(it.decodeToString()) }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.getValue(HeartcastBleService.userNameCharacteristic).collect {
                            userName = runCatching { it.decodeToString() }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.getValue(HeartcastBleService.heartRateCharacteristic).collect {
                            heartRate = runCatching { HeartRate(Buffer().write(it).readInt()) }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.getValue(HeartcastBleService.heartRateLimitCharacteristic).collect {
                            heartRateLimit = runCatching { HeartRate(Buffer().write(it).readInt()) }.getOrNull()
                            emitIfPossible()
                        }
                    }
                }
            }
        }
}