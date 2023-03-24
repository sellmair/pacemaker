@file:Suppress("OPT_IN_USAGE")

package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.bluetooth.HeartRateBleService.heartRateCharacteristic
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.HeartRateSensorInfo
import kotlinx.coroutines.flow.*
import kotlin.time.TimeSource


suspend fun Ble.receiveHeartRateMeasurements(): Flow<HeartRateMeasurement> {
    return startClient(HeartRateBleService.service)
        .peripherals
        .onEach { println("Found peripheral: ${it.peripheralId}") }
        .map { it.connect() }
        .flatMapMerge { connection ->
            connection.getValue(heartRateCharacteristic).mapNotNull { data ->
                if (data.isEmpty()) return@mapNotNull null
                val isUInt16 = data[0] == 1.toByte()
                val heartRateValue = if (isUInt16) getUShort(
                    data.getOrNull(1) ?: return@mapNotNull null,
                    data.getOrNull(2) ?: return@mapNotNull null
                ).toInt() else data.getOrNull(1)?.toUByte()?.toInt() ?: return@mapNotNull null

                HeartRateMeasurement(
                    heartRate = HeartRate(heartRateValue),
                    receivedTime = TimeSource.Monotonic.markNow(),
                    sensorInfo = HeartRateSensorInfo(
                        id = HeartRateSensorId(connection.peripheralId.value),
                        address = null,
                        vendor = HeartRateSensorInfo.Vendor.Unknown,
                        rssi = null
                    )
                )
            }
        }
}

private fun getUShort(byte1: Byte, byte2: Byte): UShort {
    return (byte1.toInt() shl 8 or byte2.toInt()).toUShort()
}
