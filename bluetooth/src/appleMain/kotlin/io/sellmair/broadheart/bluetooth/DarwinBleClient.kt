package io.sellmair.broadheart.bluetooth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okio.ByteString.Companion.toByteString
import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject

suspend fun DarwinBleClient(scope: CoroutineScope, service: BleServiceDescriptor): BleClient {
    val centralDelegate = CentralDelegate(scope)
    val cbCentralManager = CBCentralManager(centralDelegate, null)
    centralDelegate.awaitStatePoweredOn()

    cbCentralManager.scanForPeripheralsWithServices(listOf(service.uuid), mutableMapOf<Any?, Any>())

    return object : BleClient {
        override val service: BleServiceDescriptor = service
        override val peripherals: Flow<BlePeripheral>
            get() = centralDelegate.peripherals
                .map { peripheral: CBPeripheral ->
                    object : BlePeripheral {
                        override val peripheralId: BlePeripheralId = peripheral.peripheralId

                        override suspend fun connect(): BleClientConnection {
                            cbCentralManager.connectPeripheral(peripheral, mutableMapOf<Any?, Any>())

                            centralDelegate.connectedPeripherals.first { it == peripheral }
                            val peripheralDelegate = PeripheralDelegate(scope, service)
                            peripheral.delegate = peripheralDelegate
                            peripheral.discoverServices(listOf(service.uuid))

                            return object : BleClientConnection {
                                override val peripheralId: BlePeripheralId = peripheral.peripheralId

                                override fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray> {
                                    return peripheralDelegate.valueFlowOf(characteristic.uuid)
                                }
                            }
                        }
                    }
                }
    }
}

private class CentralDelegate(private val scope: CoroutineScope) : NSObject(), CBCentralManagerDelegateProtocol {
    private val state = MutableStateFlow<Long?>(null)
    private val _peripherals = MutableSharedFlow<CBPeripheral>()
    val peripherals = _peripherals.asSharedFlow()

    private val _connectedPeripherals = MutableSharedFlow<CBPeripheral>()
    val connectedPeripherals = _connectedPeripherals.asSharedFlow()

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        state.value = central.state
    }

    override fun centralManager(
        central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber
    ) {
        if (didDiscoverPeripheral.state == CBPeripheralStateDisconnected) {
            scope.launch {
                _peripherals.emit(didDiscoverPeripheral)
            }
        }
    }

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        scope.launch {
            _connectedPeripherals.emit(didConnectPeripheral)
        }
    }


    suspend fun awaitStatePoweredOn() {
        state.filterNotNull().first { it == CBCentralManagerStatePoweredOn }
    }
}

private class PeripheralDelegate(
    private val scope: CoroutineScope,
    private val service: BleServiceDescriptor
) : NSObject(), CBPeripheralDelegateProtocol {

    private val readCharacteristicChannel = Channel<Unit>()

    private val valueFlows = mutableMapOf<BleUUID, MutableStateFlow<ByteArray?>>()

    private fun stateFlowOf(uuid: BleUUID): MutableStateFlow<ByteArray?> {
        return valueFlows.getOrPut(uuid) { MutableStateFlow(null) }
    }

    fun valueFlowOf(uuid: BleUUID): Flow<ByteArray> {
        return stateFlowOf(uuid).filterNotNull()
    }

    @Suppress("UNCHECKED_CAST")
    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        if (didDiscoverServices != null) {
            println("didDiscoverServices error: $didDiscoverServices")
            return
        }
        val cbService = (peripheral.services as List<CBService>).find { it.UUID == service.uuid } ?: return
        peripheral.discoverCharacteristics(service.characteristics.map { it.uuid }, cbService)
    }

    @Suppress("UNCHECKED_CAST")
    override fun peripheral(
        peripheral: CBPeripheral,
        didDiscoverCharacteristicsForService: CBService,
        error: NSError?
    ) {
        if (didDiscoverCharacteristicsForService.UUID != service.uuid) return
        if (error != null) {
            println("didDiscoverCharacteristicsForService error: $error")
            return
        }

        scope.launch {

            /* Read all readable characteristics eagerly */
            service.characteristics.filter { it.isReadable }.forEach { characteristic ->
                val cbCharacteristic = (didDiscoverCharacteristicsForService.characteristics as List<CBCharacteristic>)
                    .find { it.UUID == characteristic.uuid } ?: return@forEach
                peripheral.readValueForCharacteristic(cbCharacteristic)
                readCharacteristicChannel.receive()
            }

            /* Enable notifications */
            service.characteristics.filter { it.isNotificationsEnabled }.forEach { characteristic ->
                val cbCharacteristic = (didDiscoverCharacteristicsForService.characteristics as List<CBCharacteristic>)
                    .find { it.UUID == characteristic.uuid } ?: return@forEach
                peripheral.setNotifyValue(true, cbCharacteristic)
            }
        }
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun peripheral(
        peripheral: CBPeripheral,
        didUpdateValueForCharacteristic: CBCharacteristic,
        error: NSError?
    ) {

        scope.launch {
            readCharacteristicChannel.send(Unit)
        }

        if (error != null) {
            println("didUpdateValueForCharacteristic error: ${error.localizedDescription}")
            return
        }

        val value = didUpdateValueForCharacteristic.value ?: return
        stateFlowOf(didUpdateValueForCharacteristic.UUID).value = value.toByteString().toByteArray()
    }
}

private val CBPeripheral.peripheralId: BlePeripheralId get() = BlePeripheralId(this.identifier.UUIDString)