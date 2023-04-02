package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.BleWritable
import io.sellmair.pacemaker.ble.PacemakerBleWritable
import io.sellmair.pacemaker.bluetooth.PacemakerBleService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.encodeToByteArray

internal class PacemakerBleWritableImpl(
    private val underlying: BleWritable
) : PacemakerBleWritable {
    override suspend fun setUser(user: User) {
        underlying.setValue(PacemakerBleService.userNameCharacteristic, user.name.encodeToByteArray())
        underlying.setValue(PacemakerBleService.userIdCharacteristic, user.id.encodeToByteArray())
    }

    override suspend fun setHeartRate(sensorId: HeartRateSensorId, heartRate: HeartRate) {
        underlying.setValue(PacemakerBleService.sensorIdCharacteristic, sensorId.value.encodeToByteArray())
        underlying.setValue(PacemakerBleService.heartRateCharacteristic, heartRate.encodeToByteArray())
    }

    override suspend fun setHeartRateLimit(heartRate: HeartRate) {
        underlying.setValue(PacemakerBleService.heartRateLimitCharacteristic, heartRate.encodeToByteArray())
    }
}