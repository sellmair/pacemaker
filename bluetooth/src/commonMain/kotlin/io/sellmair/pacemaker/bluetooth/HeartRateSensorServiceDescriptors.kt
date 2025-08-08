package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleCharacteristicDescriptor
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import io.sellmair.pacemaker.ble.BleUUID

object HeartRateSensorServiceDescriptors {
    val heartRateCharacteristic = BleCharacteristicDescriptor(
        name = "Heart Rate",
        uuid = BleUUID("00002a37-0000-1000-8000-00805f9b34fb"),
        isReadable = false,
        isNotificationsEnabled = true
    )

    val service = BleServiceDescriptor(
        name = "Heart Rate Service",
        uuid = BleUUID("0000180D-0000-1000-8000-00805f9b34fb"),
        characteristics = setOf(heartRateCharacteristic)
    )
}
