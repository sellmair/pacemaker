@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.bluetooth.HeartRateBleService.heartRateCharacteristic
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlin.time.TimeSource

interface BluetoothHeartRateSensorService {
    val sensors: SharedFlow<BluetoothHeartRateSensor>
}

interface BluetoothHeartRateSensor : BleConnectable {
    val heartRate: Flow<HeartRateMeasurement>
}

suspend fun BluetoothHeartRateSensorService(ble: Ble): BluetoothHeartRateSensorService {
    val centralService = ble.createCentralService(HeartRateBleService.service)
    centralService.startScanning()

    return object : BluetoothHeartRateSensorService {
        override val sensors = centralService.connectables.map { connectable ->
            BluetoothHeartRateSensorImpl(connectable)
        }.shareIn(ble.scope, SharingStarted.WhileSubscribed())

    }
}

private class BluetoothHeartRateSensorImpl(
    private val connectable: BleConnectable
) : BluetoothHeartRateSensor, BleConnectable by connectable {
    override val heartRate: Flow<HeartRateMeasurement> = connection.flatMapLatest { connection ->
        connection.enableNotifications(heartRateCharacteristic)
        connection.receivedValues.filter { it.characteristic == heartRateCharacteristic }
            .mapNotNull { received ->
                HeartRateMeasurement(
                    heartRate = decodeHeartRate(received.data) ?: return@mapNotNull null,
                    sensorInfo = HeartRateSensorInfo(
                        id = connection.deviceId.toHeartRateSensorId(),
                        rssi = null
                    ),
                    receivedTime = TimeSource.Monotonic.markNow()
                )
            }
    }
}


private fun getUShort(byte1: Byte, byte2: Byte): UShort {
    return (byte1.toInt() shl 8 or byte2.toInt()).toUShort()
}

private fun decodeHeartRate(data: ByteArray): HeartRate? {
    if (data.isEmpty()) return null
    val isUInt16 = data[0] == 1.toByte()
    val heartRateValue = if (isUInt16) getUShort(
        data.getOrNull(1) ?: return null,
        data.getOrNull(2) ?: return null
    ).toInt() else data.getOrNull(1)?.toUByte()?.toInt() ?: return null
    return HeartRate(heartRateValue)
}