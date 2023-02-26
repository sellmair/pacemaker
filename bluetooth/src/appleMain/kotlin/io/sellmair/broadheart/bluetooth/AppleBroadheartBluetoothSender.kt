package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.User
import kotlinx.cinterop.*
import platform.CoreBluetooth.*
import platform.CoreBluetooth.CBUUID.Companion.UUIDWithString
import platform.Foundation.*

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

    val heartRateMeasurementCharacteristic = CBMutableCharacteristic(
        type = UUIDWithString(ServiceConstants.heartRateMeasurementCharacteristicUuidString),
        properties = CBCharacteristicPropertyRead or CBCharacteristicPropertyNotify,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    service.setCharacteristics(
        listOf(userIdCharacteristic, userNameCharacteristic, heartRateMeasurementCharacteristic)
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

        override fun updateHeartHeart(heartRate: HeartRate) {
            peripheral.manager
        }

        override fun updateHeartRateLimit(heartRate: HeartRate) {
            peripheral.manager
        }

        override fun toString(): String {
            return "$peripheral/${peripheral.manager}/${peripheral.manager.delegate}"
        }
    }
}