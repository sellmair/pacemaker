package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.UserId
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okio.Buffer
import kotlin.time.TimeSource

interface HeartcastBleCentralService : BleCentralService {
    override val peripherals: Flow<HeartcastBlePeripheral>
}

interface HeartcastBlePeripheral : BlePeripheral {
    val broadcasts: Flow<HeartcastBroadcastPackage>
}

suspend fun Ble.startHeartcastBleCentralService(): HeartcastBleCentralService {
    return HeartcastBleCentralServiceImpl(startCentralService(HeartcastBleService.service))
}

private class HeartcastBleCentralServiceImpl(
    private val centralService: BleCentralService
) : HeartcastBleCentralService, BleCentralService by centralService {
    override val peripherals: Flow<HeartcastBlePeripheral> = centralService.peripherals
        .map(::HeartcastBlePeripheralImpl)
}

private class HeartcastBlePeripheralImpl(
    private val peripheral: BlePeripheral
) : HeartcastBlePeripheral,
    BlePeripheral by peripheral {
    override val broadcasts: Flow<HeartcastBroadcastPackage> = channelFlow {
        var userId: UserId? = null
        var sensorId: HeartRateSensorId? = null
        var userName: String? = null
        var heartRate: HeartRate? = null
        var heartRateLimit: HeartRate? = null

        suspend fun emitIfPossible() {
            send(
                HeartcastBroadcastPackage(
                    receivedTime = TimeSource.Monotonic.markNow(),
                    deviceId = peripheral.id,
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
