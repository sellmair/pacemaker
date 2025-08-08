package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import io.sellmair.pacemaker.bluetooth.HeartRateSensor
import io.sellmair.pacemaker.bluetooth.HeartRateSensorMeasurement
import io.sellmair.pacemaker.bluetooth.HeartRateSensorServiceDescriptors
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.HeartRateSensorInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun CoroutineScope.launchHeartRateSensorEmulation(): Job = launch {
    val mySensor = object : HeartRateSensor {
        override val heartRate = MutableSharedFlow<HeartRateSensorMeasurement>()

        init {
            launch {
                var currentHeartRate = HeartRate(Random.nextInt(50, 100))
                var drift = 0f

                launch {
                    while (isActive) {
                        delay(2.seconds)
                        drift = (Random.nextFloat() - 0.5f) * 0.5f
                    }
                }

                while (isActive) {
                    delay(128.milliseconds)
                    val delta = (Random.nextFloat() - 0.5f) + drift
                    currentHeartRate = HeartRate(Math.clamp(currentHeartRate.value + delta, 40f, 160f))

                    heartRate.emit(
                        HeartRateSensorMeasurement(
                            heartRate = currentHeartRate,
                            sensorInfo = HeartRateSensorInfo(HeartRateSensorId(deviceId.value)),
                            receivedTime = Clock.System.now()
                        )
                    )
                }
            }
        }

        override val deviceName: String = "Emulated"

        override val deviceId: BleDeviceId
            get() = BleDeviceId("Emulated")

        override val service: BleServiceDescriptor
            get() = HeartRateSensorServiceDescriptors.service

        private val _connection = MutableStateFlow<BleConnection?>(null)

        override val connection: SharedFlow<BleConnection> = _connection
            .filterNotNull()
            .shareIn(this@launch, SharingStarted.Eagerly)

        override val connectionState = MutableStateFlow(BleConnectable.ConnectionState.Disconnected)

        override val connectIfPossible = MutableStateFlow(false)

        override fun connectIfPossible(connect: Boolean) {
            connectIfPossible.value = connect

            connectionState.value = if (connect) BleConnectable.ConnectionState.Connected
            else BleConnectable.ConnectionState.Disconnected
        }

        override val rssi = MutableStateFlow<Int?>(24)

    }

    JvmHeartRateSensorBluetoothService.addSensor(mySensor)
}
