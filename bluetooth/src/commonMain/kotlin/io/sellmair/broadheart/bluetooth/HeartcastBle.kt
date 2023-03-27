@file:OptIn(FlowPreview::class)

package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import okio.Buffer
import kotlin.math.roundToInt

interface HeartcastBle {
    val connections: Flow<BleConnection>
    val receivedPackages: Flow<HeartcastBroadcastPackage>
    suspend fun updateUser(user: User)
    suspend fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate)
    suspend fun updateHeartRateLimit(heartRate: HeartRate)
}

suspend fun HeartcastBle(ble: Ble): HeartcastBle {
    val centralService = ble.startCentralService(HeartcastBleService.service)
    val peripheralService = ble.startPeripheralService(HeartcastBleService.service)

    suspend fun forService(action: suspend (service: BleService) -> Unit) {
        action(peripheralService)
        action(centralService)
    }

    return object : HeartcastBle {
        val centralService = centralService
        val peripheralService = peripheralService

        override val connections: Flow<BleConnection> = flowOf(
            this.peripheralService.centrals, this.centralService.peripherals.onEach { it.tryConnect() }
        ).flattenMerge().shareIn(ble.scope, SharingStarted.Eagerly)

        override val receivedPackages: Flow<HeartcastBroadcastPackage> = connections
            .flatMapMerge { it.receiveHeartcastBroadcastPackages() }

        override suspend fun updateUser(user: User) {
            forService { service ->
                service.setValue(HeartcastBleService.userNameCharacteristic, user.name.encodeToByteArray())
                service.setValue(
                    HeartcastBleService.userIdCharacteristic,
                    Buffer().writeLong(user.id.value).readByteArray()
                )
            }
        }

        override suspend fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate) {
            forService { service ->
                service.setValue(
                    HeartcastBleService.heartRateCharacteristic,
                    Buffer().writeInt(heartRate.value.roundToInt()).readByteArray()
                )

                service.setValue(
                    HeartcastBleService.sensorIdCharacteristic,
                    sensorId.value.encodeToByteArray()
                )
            }
        }

        override suspend fun updateHeartRateLimit(heartRate: HeartRate) {
            forService { service ->
                service.setValue(
                    HeartcastBleService.heartRateLimitCharacteristic,
                    Buffer().writeInt(heartRate.value.roundToInt()).readByteArray()
                )
            }
        }
    }
}