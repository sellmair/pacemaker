package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.User
import platform.CoreBluetooth.*
import platform.CoreBluetooth.CBUUID.Companion.UUIDWithString
import platform.Foundation.*
import kotlin.math.roundToInt

suspend fun BroadheartBluetoothSender(user: User): BroadheartBluetoothSender {
    val peripheral = Peripheral()
    peripheral.awaitBluetoothPoweredOn()

    val serviceUUID = UUIDWithString(ServiceConstants.serviceUuidString)
    val service = CBMutableService(serviceUUID, true)

    val userIdCharacteristic = CBMutableCharacteristic(
        type = UUIDWithString(ServiceConstants.userIdCharacteristicUuidString),
        properties = CBCharacteristicPropertyRead,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    val userNameCharacteristic = CBMutableCharacteristic(
        type = UUIDWithString(ServiceConstants.userNameCharacteristicUuidString),
        properties = CBCharacteristicPropertyRead,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    val sensorIdCharacteristic = CBMutableCharacteristic(
        type = UUIDWithString(ServiceConstants.sensorIdCharacteristicUuidString),
        properties = CBCharacteristicPropertyRead or CBCharacteristicPropertyNotify,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    val heartRateCharacteristic = CBMutableCharacteristic(
        type = UUIDWithString(ServiceConstants.heartRateCharacteristicUuidString),
        properties = CBCharacteristicPropertyRead or CBCharacteristicPropertyNotify,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    val heartRateLimitCharacteristic = CBMutableCharacteristic(
        type = UUIDWithString(ServiceConstants.heartRateLimitCharacteristicUuidString),
        properties = CBCharacteristicPropertyRead or CBCharacteristicPropertyNotify,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    service.setCharacteristics(
        listOf(
            userIdCharacteristic,
            userNameCharacteristic,
            sensorIdCharacteristic,
            heartRateCharacteristic,
            heartRateLimitCharacteristic
        )
    )

    peripheral.manager.addService(service)

    peripheral.manager.startAdvertising(
        mapOf(
            CBAdvertisementDataLocalNameKey to NSString.create(string = "Broadheart"),
            CBAdvertisementDataServiceUUIDsKey to NSArray.create(listOf(serviceUUID)),
        )
    )

    peripheral.userId = user.id.value
    peripheral.userName = user.name


    return object : BroadheartBluetoothSender {
        override fun updateUser(user: User) {
            peripheral.userId = user.id.value
            peripheral.userName = user.name
        }

        override fun updateHeartHeart(sensorId: HeartRateSensorId, heartRate: HeartRate) {
            peripheral.sensorId = sensorId.value
            peripheral.heartRate = heartRate.value.roundToInt()

            peripheral.manager.updateValue(
                heartRate.value.roundToInt().toNSData(),
                heartRateCharacteristic, null
            )

            peripheral.manager.updateValue(
                sensorId.value.toNSData(),
                sensorIdCharacteristic, null
            )
        }

        override fun updateHeartRateLimit(heartRate: HeartRate) {
            peripheral.heartRateLimit = heartRate.value.roundToInt()
            peripheral.manager.updateValue(
                heartRate.value.roundToInt().toNSData(),
                heartRateLimitCharacteristic, null
            )
        }

        override fun toString(): String {
            return "$peripheral/${peripheral.manager}/${peripheral.manager.delegate}"
        }
    }
}
