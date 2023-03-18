package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import okio.Buffer
import kotlin.math.roundToInt

interface HeartcastBluetoothSender {
    suspend fun updateUser(user: User)
    suspend fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate)
    suspend fun updateHeartRateLimit(heartRate: HeartRate)
}

suspend fun HeartcastBluetoothSender(ble: Ble): HeartcastBluetoothSender {
    val server = ble.startServer(HeartcastBleService.service)
    return object : HeartcastBluetoothSender {
        override suspend fun updateUser(user: User) {
            server.setValue(HeartcastBleService.userNameCharacteristic, user.name.encodeToByteArray())
            server.setValue(HeartcastBleService.userIdCharacteristic, Buffer().writeLong(user.id.value).readByteArray())
        }

        override suspend fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate) {
            server.setValue(
                HeartcastBleService.heartRateCharacteristic,
                Buffer().writeInt(heartRate.value.roundToInt()).readByteArray()
            )

            server.setValue(
                HeartcastBleService.sensorIdCharacteristic,
                sensorId.value.encodeToByteArray()
            )
        }

        override suspend fun updateHeartRateLimit(heartRate: HeartRate) {
            server.setValue(
                HeartcastBleService.heartRateLimitCharacteristic,
                Buffer().writeInt(heartRate.value.roundToInt()).readByteArray()
            )
        }
    }
}