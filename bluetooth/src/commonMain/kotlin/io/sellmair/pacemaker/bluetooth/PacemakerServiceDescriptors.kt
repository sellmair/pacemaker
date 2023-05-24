package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleCharacteristicDescriptor
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import io.sellmair.pacemaker.ble.BleUUID

 internal object PacemakerServiceDescriptors {
    val userIdCharacteristic = BleCharacteristicDescriptor(
        name = "userId",
        uuid = BleUUID(PacemakerServiceConstants.userIdCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = false
    )

    val userNameCharacteristic = BleCharacteristicDescriptor(
        name = "userName",
        uuid = BleUUID(PacemakerServiceConstants.userNameCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = false
    )

    val sensorIdCharacteristic = BleCharacteristicDescriptor(
        name = "sensorId",
        uuid = BleUUID(PacemakerServiceConstants.sensorIdCharacteristicUuidString),
        isReadable = false,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val heartRateCharacteristic = BleCharacteristicDescriptor(
        name = "heartRate",
        uuid = BleUUID(PacemakerServiceConstants.heartRateCharacteristicUuidString),
        isReadable = false,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val heartRateLimitCharacteristic = BleCharacteristicDescriptor(
        name = "heartRateLimit",
        uuid = BleUUID(PacemakerServiceConstants.heartRateLimitCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val service = BleServiceDescriptor(
        name = "Pacemaker App",
        uuid = BleUUID(PacemakerServiceConstants.serviceUuidString),
        characteristics = setOf(
            userIdCharacteristic,
            userNameCharacteristic,
            sensorIdCharacteristic,
            heartRateCharacteristic,
            heartRateLimitCharacteristic
        )
    )
}

