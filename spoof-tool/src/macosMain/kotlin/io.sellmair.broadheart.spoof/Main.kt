package io.sellmair.broadheart.spoof

import io.sellmair.broadheart.bluetooth.BroadheartBluetoothSender
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.User
import io.sellmair.broadheart.model.UserId
import io.sellmair.broadheart.model.randomUserId
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toCValues
import kotlinx.coroutines.*
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import platform.CoreBluetooth.*
import platform.CoreFoundation.CFRunLoopRun
import platform.Foundation.*
import platform.darwin.NSObject
import platform.posix.send

var sender: BroadheartBluetoothSender? = null
fun main() {
    MainScope().launch(Dispatchers.Main) {
        val user = User(
            isMe = true,
            id = UserId(2412),
            name = "Felix Werner"
        )

        sender = BroadheartBluetoothSender(user)

        while (true) {
            delay(10000)
            println(sender)
        }
    }

    CFRunLoopRun()
}
/*

// Create a new CBPeripheralManager object
val peripheralManager = CBPeripheralManager(null, null)
var peripheralManagerDelegate: CBPeripheralManagerDelegateProtocol? = null


val serviceUUID = CBUUID.UUIDWithString("35b6d6ed-b85f-48e6-8f21-26c9877dbbe8")
val characteristicUUID = CBUUID.UUIDWithString("c76b2e24-a272-4a7f-bd66-7acad9bd0014")


private suspend fun broadcastFile(path: Path) {

    val serviceUUID = CBUUID.UUIDWithString("35b6d6ed-b85f-48e6-8f21-26c9877dbbe8")

    while (peripheralManager.state != CBPeripheralManagerStatePoweredOn) {
        println("Waiting for bluetooth!")
        delay(1000)
    }

    println("Bluetooth ready")

    val service = CBMutableService(serviceUUID, true)


    val characteristic = CBMutableCharacteristic(
        type = characteristicUUID, properties = CBCharacteristicPropertyNotify or CBCharacteristicPropertyRead,
        value = null, permissions = CBAttributePermissionsReadable or CBAttributePermissionsWriteable
    )

    service.setCharacteristics(listOf(characteristic))

    peripheralManager.addService(service)

    MainScope().launch {
        while (true) {
            val data = FileSystem.SYSTEM.source(path).buffer().readByteArray()
            val nsData = memScoped {
                NSData.create(bytes = data.toCValues().ptr, data.size.toULong())
            }
            peripheralManager.updateValue(nsData, characteristic, null)
            delay(1000)
        }
    }

    val delegate = object : NSObject(), CBPeripheralManagerDelegateProtocol {
        override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
            println("State changed: ${peripheral.state}")
        }

        override fun peripheralManagerDidStartAdvertising(peripheral: CBPeripheralManager, error: NSError?) {
            if (error != null) error(error.localizedDescription)
            println("Advertising started")
        }

        override fun peripheralManager(
            peripheral: CBPeripheralManager,
            central: CBCentral,
            didSubscribeToCharacteristic: CBCharacteristic
        ) {
            println("Somebody (${central.maximumUpdateValueLength}) subscribed!")
        }

        override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
            println("Received read request")
            println("Received read request (${didReceiveReadRequest.characteristic.UUID})")
            if (didReceiveReadRequest.characteristic.UUID != characteristicUUID) return

            val data = FileSystem.SYSTEM.source(path).buffer().readByteArray()
            val nsData = memScoped {
                NSData.create(bytes = data.toCValues().ptr, data.size.toULong())
            }

            if (didReceiveReadRequest.offset > nsData.length) {
                peripheralManager.respondToRequest(didReceiveReadRequest, CBATTErrorInvalidOffset)
            }

            didReceiveReadRequest.setValue(
                nsData.subdataWithRange(
                    NSMakeRange(
                        didReceiveReadRequest.offset,
                        nsData.length - didReceiveReadRequest.offset
                    )
                )
            )
            peripheralManager.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
        }

        override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveWriteRequests: List<*>) {
            println("Received write request!")
        }

        override fun peripheralManager(peripheral: CBPeripheralManager, didAddService: CBService, error: NSError?) {
            if (error != null) error(error.localizedDescription)
        }
    }

    peripheralManager.delegate = delegate
    peripheralManagerDelegate = delegate

    peripheralManager.startAdvertising(
        mapOf(
            CBAdvertisementDataLocalNameKey to NSString.create(string = "BH"),
            CBAdvertisementDataServiceUUIDsKey to NSArray.create(listOf(serviceUUID)),
        )
    )
}


 */