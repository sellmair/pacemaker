package io.sellmair.pacemaker.bluetooth

internal object PacemakerBleService {
    val userIdCharacteristic = BleCharacteristicDescriptor(
        name = "userId",
        uuid = BleUUID(PacemakerBleServiceConstants.userIdCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = false
    )

    val userNameCharacteristic = BleCharacteristicDescriptor(
        name = "userName",
        uuid = BleUUID(PacemakerBleServiceConstants.userNameCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = false
    )

    val sensorIdCharacteristic = BleCharacteristicDescriptor(
        name = "sensorId",
        uuid = BleUUID(PacemakerBleServiceConstants.sensorIdCharacteristicUuidString),
        isReadable = false,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val heartRateCharacteristic = BleCharacteristicDescriptor(
        name = "heartRate",
        uuid = BleUUID(PacemakerBleServiceConstants.heartRateCharacteristicUuidString),
        isReadable = false,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val heartRateLimitCharacteristic = BleCharacteristicDescriptor(
        name = "heartRateLimit",
        uuid = BleUUID(PacemakerBleServiceConstants.heartRateLimitCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val service = BleServiceDescriptor(
        name = "Pacemaker App",
        uuid = BleUUID(PacemakerBleServiceConstants.serviceUuidString),
        characteristics = setOf(
            userIdCharacteristic,
            userNameCharacteristic,
            sensorIdCharacteristic,
            heartRateCharacteristic,
            heartRateLimitCharacteristic
        )
    )
}

