package io.sellmair.broadheart.bluetooth

import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import okio.Buffer
import platform.CoreBluetooth.*
import platform.CoreBluetooth.CBUUID.Companion.UUIDWithString
import platform.Foundation.*
import platform.darwin.NSObject

internal class Peripheral : NSObject(), CBPeripheralManagerDelegateProtocol {
    val manager = CBPeripheralManager(this, null)
    private val peripheralManagerState = MutableStateFlow(CBPeripheralManagerStateUnknown)

    var userName: String? = null
    var userId: Long? = null
    var sensorId: String? = null
    var heartRate: Int? = null
    var heartRateLimit: Int? = null

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        peripheralManagerState.value = peripheral.state
    }

    override fun peripheralManagerDidStartAdvertising(peripheral: CBPeripheralManager, error: NSError?) {
        if (error != null) error(error.localizedDescription)
        println("Advertising started")
    }

    override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
        println("Received read request (${didReceiveReadRequest.characteristic.UUID})")
        val data: NSData = when (didReceiveReadRequest.characteristic.UUID) {
            UUIDWithString(ServiceConstants.userIdCharacteristicUuidString) -> {
                println("Sending userId=$userId")
                userId?.toNSData()
                    ?: return manager.respondToRequest(didReceiveReadRequest, CBATTErrorUnlikelyError)
            }

            UUIDWithString(ServiceConstants.userNameCharacteristicUuidString) -> {
                println("Sending userName=$userName")
                userName?.toNSData()
                    ?: return manager.respondToRequest(didReceiveReadRequest, CBATTErrorUnlikelyError)
            }

            UUIDWithString(ServiceConstants.sensorIdCharacteristicUuidString) -> {
                println("Sending sensorId=$sensorId")
                sensorId?.toNSData()
                    ?: return manager.respondToRequest(didReceiveReadRequest, CBATTErrorAttributeNotFound)
            }

            UUIDWithString(ServiceConstants.heartRateCharacteristicUuidString) -> {
                println("Sending heartRate=$heartRate")
                heartRate?.toNSData()
                    ?: return manager.respondToRequest(didReceiveReadRequest, CBATTErrorAttributeNotFound)
            }

            UUIDWithString(ServiceConstants.heartRateLimitCharacteristicUuidString) -> {
                println("Sending heartRateLimit=$heartRateLimit")
                heartRateLimit?.toNSData()
                    ?: return manager.respondToRequest(didReceiveReadRequest, CBATTErrorAttributeNotFound)
            }

            else -> {
                println("Requested unknown characteristic: ${didReceiveReadRequest.characteristic.UUID}")
                manager.respondToRequest(didReceiveReadRequest, CBATTErrorAttributeNotFound)
                return
            }
        }
        if (didReceiveReadRequest.offset > data.length) {
            manager.respondToRequest(didReceiveReadRequest, CBATTErrorInvalidOffset)
            return
        }

        didReceiveReadRequest.setValue(
            data.subdataWithRange(
                NSMakeRange(
                    didReceiveReadRequest.offset,
                    data.length - didReceiveReadRequest.offset
                )
            )
        )
        manager.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
    }

    suspend fun awaitBluetoothPoweredOn() {
        peripheralManagerState.filter { it == CBPeripheralManagerStatePoweredOn }.first()
    }
}

internal fun Long.toNSData(): NSData {
    val buffer = Buffer()
    buffer.writeLong(this)
    return buffer.readByteArray().toNSData()
}

internal fun Int.toNSData(): NSData {
    val buffer = Buffer()
    buffer.writeInt(this)
    return buffer.readByteArray().toNSData()
}

internal fun String.toNSData(): NSData {
    return encodeToByteArray().toNSData()
}

internal fun ByteArray.toNSData(): NSData {
    return memScoped {
        NSData.create(bytes = this@toNSData.toCValues().ptr, length = size.toULong())
    }
}