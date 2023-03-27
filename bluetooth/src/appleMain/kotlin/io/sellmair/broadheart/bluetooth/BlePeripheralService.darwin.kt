@file:Suppress("FunctionName")

package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.utils.toNSData
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okio.ByteString.Companion.toByteString
import platform.CoreBluetooth.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.native.internal.createCleaner

@OptIn(ExperimentalStdlibApi::class)
suspend fun BlePeripheralService(service: BleServiceDescriptor): BlePeripheralService {
    val peripheralDelegate = CBPeripheralManagerDelegate()
    val cbPeripheralManager = CBPeripheralManager(peripheralDelegate, null)
    peripheralDelegate.awaitPoweredOnState()

    val cbService = CBMutableService(service.uuid, true)
    val cbCharacteristics = service.characteristics.map { characteristic ->
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

    cbService.setCharacteristics(cbCharacteristics)
    cbPeripheralManager.addService(cbService)

    cbPeripheralManager.startAdvertising(
        mapOf(
            CBAdvertisementDataLocalNameKey to NSString.create(string = service.name.orEmpty() + "APPLE"),
            CBAdvertisementDataServiceUUIDsKey to NSArray.create(listOf(service.uuid)),
        )
    )

    return object : BlePeripheralService {

        @Suppress("unused")
        private val peripheralManagerCleaner =
            createCleaner(cbPeripheralManager) { println("Cleaning peripheral manager") }

        @Suppress("unused")
        private val peripheralDelegateCleaner =
            createCleaner(peripheralDelegate) { println("Cleaning peripheral manager delegate") }


        override val service: BleServiceDescriptor = service

        override val centrals: Flow<BleConnection> = peripheralDelegate.connectionsFlow

        override suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
            val cbCharacteristic = cbCharacteristics.find { it.UUID == characteristic.uuid } ?: return
            val nsData = value.toNSData()

            if (characteristic.isReadable) {
                cbCharacteristic.setValue(nsData)
            }

            if (characteristic.isNotificationsEnabled) {
                while (!cbPeripheralManager.updateValue(nsData, cbCharacteristic, null)) {
                    println("Ble: peripheral service: update value operation")
                    peripheralDelegate.isReadyToUpdateSubscribersChannel.receive()
                    println("Ble: notification channel is free!")
                }
            }
        }
    }
}

private class CBPeripheralManagerDelegate : NSObject(), CBPeripheralManagerDelegateProtocol {
    private val _state = MutableStateFlow<Long?>(null)
    val state: StateFlow<Long?> = _state.asStateFlow()

    val connections = mutableMapOf<BleDeviceId, CentralConnection>()
    val connectionsFlow = MutableSharedFlow<CentralConnection>(replay = Channel.UNLIMITED)

    val isReadyToUpdateSubscribersChannel = Channel<Unit>()

    inner class CentralConnection(central: CBCentral) : BleConnection {
        private val valueFlows = mutableMapOf<BleUUID, MutableSharedFlow<ByteArray>>()

        override val id: BleDeviceId = central.deviceId

        fun valueFlowOf(uuid: BleUUID): MutableSharedFlow<ByteArray> {
            return valueFlows.getOrPut(uuid) {
                MutableSharedFlow(replay = 1, onBufferOverflow = DROP_OLDEST)
            }
        }

        override fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray> {
            return valueFlowOf(characteristic.uuid)
        }
    }

    override fun peripheralManagerIsReadyToUpdateSubscribers(peripheral: CBPeripheralManager) {
        isReadyToUpdateSubscribersChannel.trySend(Unit)
    }

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        _state.value = peripheral.state
        println("peripheralManagerDidUpdateState: ${peripheral.state}")
    }

    suspend fun awaitPoweredOnState() {
        state.filter { it == CBPeripheralManagerStatePoweredOn }.first()
    }

    override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
        println("peripheralManager: didReceiveReadRequest: $didReceiveReadRequest")

        val data = didReceiveReadRequest.characteristic.value
            ?: return peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorUnlikelyError)

        if (didReceiveReadRequest.offset > data.length) {
            peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorInvalidOffset)
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
        peripheral.respondToRequest(didReceiveReadRequest, CBATTErrorSuccess)
    }


    override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveWriteRequests: List<*>) {
        didReceiveWriteRequests.forEach { writeRequest ->
            println("peripheralManager: didReceiveWriteRequests: $writeRequest")
            writeRequest as CBATTRequest

            val centralConnection = connections.getOrPut(writeRequest.central.deviceId) {
                CentralConnection(writeRequest.central).also {
                    check(connectionsFlow.tryEmit(it))
                }
            }

            val value = writeRequest.value?.toByteString()?.toByteArray()
                ?: return@forEach println("Missing value for '${writeRequest.characteristic.value}'")

            centralConnection.valueFlowOf(writeRequest.characteristic.UUID)
                .tryEmit(value)

            peripheral.respondToRequest(writeRequest, CBATTErrorSuccess)
        }
    }


    @Suppress("unused")
    @OptIn(ExperimentalStdlibApi::class)
    private val cleaner = createCleaner(this) { println("CBPeripheralManagerDelegate deallocated") }

}