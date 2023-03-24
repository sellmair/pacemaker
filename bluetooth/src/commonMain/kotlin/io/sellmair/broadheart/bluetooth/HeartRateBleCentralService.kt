
package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.bluetooth.HeartRateBleService.heartRateCharacteristic
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.HeartRateSensorInfo
import kotlinx.coroutines.flow.*
import kotlin.time.TimeSource

interface HeartRateBleCentralService : BleCentralService {
    override val peripherals: Flow<HeartRateBlePeripheral>
}

interface HeartRateBlePeripheral : BlePeripheral {
    val heartRateMeasurements: Flow<HeartRateMeasurement>
}

suspend fun Ble.startHeartRateBleCentralService(): HeartRateBleCentralService {
    return HeartRateCentralServiceImpl(startCentralService(HeartRateBleService.service))
}

private class HeartRateCentralServiceImpl(
    private val centralService: BleCentralService
) : HeartRateBleCentralService, BleCentralService by centralService {

    override val peripherals: Flow<HeartRateBlePeripheral>
        get() = centralService.peripherals.map(::HeartRateBlePeripheralImpl)
}

private class HeartRateBlePeripheralImpl(
    private val peripheral: BlePeripheral
) : HeartRateBlePeripheral, BlePeripheral by peripheral {
    override val heartRateMeasurements: Flow<HeartRateMeasurement> =
        peripheral.getValue(heartRateCharacteristic).mapNotNull { data ->
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
                    id = HeartRateSensorId(peripheral.id.value),
                    rssi = peripheral.rssi.value.value
                )
            )
        }
}

private fun getUShort(byte1: Byte, byte2: Byte): UShort {
    return (byte1.toInt() shl 8 or byte2.toInt()).toUShort()
}
