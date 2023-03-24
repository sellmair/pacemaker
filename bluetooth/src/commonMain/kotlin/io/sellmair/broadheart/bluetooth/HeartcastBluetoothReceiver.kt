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
    return startCentralService(HeartcastBleService.service).peripherals
        .onEach { it.tryConnect() }
        .flatMapMerge { peripheral ->
            val peripheralId = peripheral.peripheralId
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
                        peripheral.getValue(HeartcastBleService.userIdCharacteristic).collect {
                            userId = runCatching { UserId(Buffer().write(it).readLong()) }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        peripheral.getValue(HeartcastBleService.sensorIdCharacteristic).collect {
                            sensorId = runCatching { HeartRateSensorId(it.decodeToString()) }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        peripheral.getValue(HeartcastBleService.userNameCharacteristic).collect {
                            userName = runCatching { it.decodeToString() }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        peripheral.getValue(HeartcastBleService.heartRateCharacteristic).collect {
                            heartRate = runCatching { HeartRate(Buffer().write(it).readInt()) }.getOrNull()
                            emitIfPossible()
                        }
                    }

                    launch {
                        peripheral.getValue(HeartcastBleService.heartRateLimitCharacteristic).collect {
                            heartRateLimit = runCatching { HeartRate(Buffer().write(it).readInt()) }.getOrNull()
                            emitIfPossible()
                        }
                    }
                }
            }
        }
}