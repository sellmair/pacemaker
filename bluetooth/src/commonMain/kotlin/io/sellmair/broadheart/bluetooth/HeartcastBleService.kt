package io.sellmair.broadheart.bluetooth

internal object HeartcastBleService {
    val userIdCharacteristic = BleCharacteristicDescriptor(
        name = "userId",
        uuid = BleUUID(HeartcastBleServiceConstants.userIdCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = false
    )

    val userNameCharacteristic = BleCharacteristicDescriptor(
        name = "userName",
        uuid = BleUUID(HeartcastBleServiceConstants.userNameCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = false
    )

    val sensorIdCharacteristic = BleCharacteristicDescriptor(
        name = "sensorId",
        uuid = BleUUID(HeartcastBleServiceConstants.sensorIdCharacteristicUuidString),
        isReadable = false,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val heartRateCharacteristic = BleCharacteristicDescriptor(
        name = "heartRate",
        uuid = BleUUID(HeartcastBleServiceConstants.heartRateCharacteristicUuidString),
        isReadable = false,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val heartRateLimitCharacteristic = BleCharacteristicDescriptor(
        name = "heartRateLimit",
        uuid = BleUUID(HeartcastBleServiceConstants.heartRateLimitCharacteristicUuidString),
        isReadable = true,
        isWritable = true,
        isNotificationsEnabled = true
    )

    val service = BleServiceDescriptor(
        name = "Heartcast",
        uuid = BleUUID(HeartcastBleServiceConstants.serviceUuidString),
        characteristics = setOf(
            userIdCharacteristic,
            userNameCharacteristic,
            sensorIdCharacteristic,
            heartRateCharacteristic,
            heartRateLimitCharacteristic
        )
    )
}

