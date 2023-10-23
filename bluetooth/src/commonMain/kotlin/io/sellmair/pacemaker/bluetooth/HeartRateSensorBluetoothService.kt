@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.ble.BleReceivedValue
import io.sellmair.pacemaker.bluetooth.HeartRateSensorServiceDescriptors.heartRateCharacteristic
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateMeasurement
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.datetime.Clock

interface HeartRateSensorBluetoothService {
    val newSensorsNearby: SharedFlow<HeartRateSensor>
    val allSensorsNearby: SharedFlow<List<HeartRateSensor>>
}

interface HeartRateSensor : BleConnectable {
    val heartRate: SharedFlow<HeartRateMeasurement>
}

suspend fun HeartRateSensorBluetoothService(ble: Ble): HeartRateSensorBluetoothService {
    val central = ble.createCentralService(HeartRateSensorServiceDescriptors.service)

    val allSensorsNearby = MutableStateFlow<List<HeartRateSensor>>(emptyList())

    val newSensorsNearby = central.connectables
        .map { connectable -> HeartRateSensorImpl(ble.coroutineScope, connectable) }
        .onEach { sensor -> allSensorsNearby.value += sensor }
        .shareIn(ble.coroutineScope, SharingStarted.Eagerly)

    central.startScanning()

    return object : HeartRateSensorBluetoothService {
        override val newSensorsNearby: SharedFlow<HeartRateSensor> = newSensorsNearby
        override val allSensorsNearby: SharedFlow<List<HeartRateSensor>> = allSensorsNearby
    }
}

private class HeartRateSensorImpl(
    scope: CoroutineScope,
    private val delegate: BleConnectable
) : HeartRateSensor,
    BleConnectable by delegate {

    override val heartRate: SharedFlow<HeartRateMeasurement> = delegate.connection
        .onEach { connection -> connection.enableNotifications(heartRateCharacteristic) }
        .flatMapLatest { connection -> connection.receivedValues }
        .filter { it.characteristic == heartRateCharacteristic }
        .mapNotNull { received -> heartRateMeasurement(received) }
        .shareIn(scope, SharingStarted.Eagerly)

    fun heartRateMeasurement(received: BleReceivedValue): HeartRateMeasurement? {
        return HeartRateMeasurement(
            heartRate = decodeHeartRate(received.data) ?: return null,
            sensorInfo = HeartRateSensorInfo(
                id = delegate.deviceId.toHeartRateSensorId(),
                rssi = null

            ),
            receivedTime = Clock.System.now()
        )
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

    private fun getUShort(byte1: Byte, byte2: Byte): UShort {
        return (byte1.toInt() shl 8 or byte2.toInt()).toUShort()
    }
}

