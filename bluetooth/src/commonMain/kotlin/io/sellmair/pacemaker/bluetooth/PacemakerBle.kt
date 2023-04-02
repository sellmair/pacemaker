@file:OptIn(FlowPreview::class)

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.encodeToByteArray
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

interface PacemakerBle {
    val connections: Flow<BleConnection>
    val receivedPackages: Flow<PacemakerBroadcastPackage>
    suspend fun updateUser(user: User)
    suspend fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate)
    suspend fun updateHeartRateLimit(heartRate: HeartRate)
}

suspend fun PacemakerBle(ble: Ble): PacemakerBle {
    val centralService = ble.startCentralService(PacemakerBleService.service)
    val peripheralService = ble.startPeripheralService(PacemakerBleService.service)

    suspend fun forService(action: suspend (service: BleService) -> Unit) {
        action(peripheralService)
        action(centralService)
    }

    return object : PacemakerBle {
        val centralService = centralService
        val peripheralService = peripheralService

        override val connections: Flow<BleConnection> = flowOf(
            this.peripheralService.centrals, this.centralService.peripherals.onEach { it.tryConnect() }
        ).flattenMerge().shareIn(ble.scope, SharingStarted.Eagerly)

        override val receivedPackages: Flow<PacemakerBroadcastPackage> = connections
            .flatMapMerge { it.receivePacemakerBroadcastPackages() }

        override suspend fun updateUser(user: User) {
            forService { service ->
                service.setValue(PacemakerBleService.userNameCharacteristic, user.name.encodeToByteArray())
                service.setValue(PacemakerBleService.userIdCharacteristic, user.id.encodeToByteArray())
            }
        }

        override suspend fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate) {
            forService { service ->
                service.setValue(
                    PacemakerBleService.heartRateCharacteristic,
                    heartRate.encodeToByteArray()
                )

                service.setValue(
                    PacemakerBleService.sensorIdCharacteristic,
                    sensorId.value.encodeToByteArray()
                )
            }
        }

        override suspend fun updateHeartRateLimit(heartRate: HeartRate) {
            forService { service ->
                service.setValue(
                    PacemakerBleService.heartRateLimitCharacteristic,
                    heartRate.encodeToByteArray()
                )
            }
        }
    }
}