package io.sellmair.broadheart.spoof

import kotlinx.cinterop.ObjCMethod
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.sleep

fun main() {
    FileSystem.SYSTEM.list("spoof".toPath()).forEach {
        broadcastFile(it)
    }
}

private fun broadcastFile(path: Path) {
    val transferCharacteristics = CBMutableCharacteristic(
        type = CBUUID.new()!!,
        properties = CBCharacteristicPropertyBroadcast,
        value = null,
        permissions = CBAttributePermissionsReadable
    )

    val serviceUUID = CBUUID.UUIDWithString("35b6d6ed-b85f-48e6-8f21-26c9877dbbe8")


    val transferService = CBMutableService(
        type = serviceUUID,
        primary = true
    )


    // Create a new CBPeripheralManager object
    val peripheralManager = CBPeripheralManager(
        object : NSObject(), CBPeripheralManagerDelegateProtocol {

            override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
                println("LAST RE")
                when (peripheral.state) {
                    CBPeripheralManagerStatePoweredOn -> {
                        println("READY!")

                        peripheral.startAdvertising(
                            mapOf(CBAdvertisementDataServiceUUIDsKey to serviceUUID)
                        )

                        val encoded = NSString.create(string = "Hello There").dataUsingEncoding(NSUTF8StringEncoding)!!

                        while (true) {
                            peripheral.updateValue(
                                encoded, transferCharacteristics, null
                            )
                            sleep(1)
                        }

                    }

                    CBPeripheralManagerStatePoweredOff -> {
                        println("Peripheral manager is powered off")
                    }

                    CBPeripheralManagerStateUnauthorized -> {
                        println("Peripheral manager is unauthorized")
                    }

                    CBPeripheralManagerStateUnsupported -> {
                        println("Peripheral manager is unsupported")
                    }

                    CBPeripheralManagerStateResetting -> {
                        println("Peripheral manager is resetting")
                    }

                    CBPeripheralManagerStateUnknown -> {
                        println("Peripheral manager state is unknown")
                    }
                }
            }
        }, null, options = mapOf(
            CBPeripheralManagerOptionShowPowerAlertKey to true
        )
    )


    val data = FileSystem.SYSTEM.source(path).buffer().readByteArray()

    val nsData = memScoped {
        NSData.create(bytes = data.toCValues().ptr, data.size.toULong())
    }


    transferService.setCharacteristics(listOf(transferCharacteristics))
    peripheralManager.addService(transferService)

    peripheralManager.startAdvertising(
        mapOf(CBAdvertisementDataServiceUUIDsKey to serviceUUID)
    )

    while (true) {
        sleep(1)
        println("Spoofing ${path}, state = ${peripheralManager.state}")
    }

}