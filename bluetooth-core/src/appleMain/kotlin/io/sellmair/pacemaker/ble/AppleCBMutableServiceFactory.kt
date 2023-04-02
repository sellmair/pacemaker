package io.sellmair.pacemaker.ble

import platform.CoreBluetooth.*


internal fun CBMutableService(descriptor: BleServiceDescriptor): CBMutableService {
    val service = CBMutableService(descriptor.uuid, true)
    val characteristics = descriptor.characteristics.map { characteristic ->
        CBMutableCharacteristic(
            type = characteristic.uuid,
            properties = ((CBCharacteristicPropertyRead.takeIf { characteristic.isReadable } ?: 0.toULong()) or
                    (CBCharacteristicPropertyNotify.takeIf { characteristic.isNotificationsEnabled } ?: 0.toULong())) or
                    (CBCharacteristicPropertyWrite.takeIf { characteristic.isWritable } ?: 0.toULong()),
            value = null,
            permissions = CBAttributePermissionsReadable or
                    (CBAttributePermissionsWriteable.takeIf { characteristic.isWritable } ?: 0.toULong())
        )
    }
    service.setCharacteristics(characteristics)
    return service
}