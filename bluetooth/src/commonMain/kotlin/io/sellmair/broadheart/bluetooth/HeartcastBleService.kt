package io.sellmair.broadheart.bluetooth

internal object HeartcastBleService {
    val userIdCharacteristic = BleCharacteristicDescriptor(
        name = "userId",
        uuid = BleUUID(HeartcastBleServiceConstants.userIdCharacteristicUuidString),
        isReadable = true,
        isNotificationsEnabled = false
    )

    val userNameCharacteristic = BleCharacteristicDescriptor(
        name = "userName",
        uuid = BleUUID(HeartcastBleServiceConstants.userNameCharacteristicUuidString),
        isReadable = true,
        isNotificationsEnabled = false
    )

    val sensorIdCharacteristic = BleCharacteristicDescriptor(
        name = "sensorId",
        uuid = BleUUID(HeartcastBleServiceConstants.sensorIdCharacteristicUuidString),
        isReadable = false,
        isNotificationsEnabled = true
    )

    val heartRateCharacteristic = BleCharacteristicDescriptor(
        name = "heartRate",
        uuid = BleUUID(HeartcastBleServiceConstants.heartRateCharacteristicUuidString),
        isReadable = false,
        isNotificationsEnabled = true
    )

    val heartRateLimitCharacteristic = BleCharacteristicDescriptor(
        name = "heartRateLimit",
        uuid = BleUUID(HeartcastBleServiceConstants.heartRateLimitCharacteristicUuidString),
        isReadable = true,
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

