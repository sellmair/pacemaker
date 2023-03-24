package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.bluetooth.BlePeripheral.State
import io.sellmair.broadheart.utils.distinct
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import okio.ByteString.Companion.toByteString
import platform.CoreBluetooth.*
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import kotlin.coroutines.CoroutineContext


suspend fun BleCentralService(scope: CoroutineScope, service: BleServiceDescriptor): BleCentralService {
    val centralDelegate = CentralDelegate(scope)
    val central = CBCentralManager(centralDelegate, null)
    centralDelegate.awaitStatePoweredOn()

    central.scanForPeripheralsWithServices(listOf(service.uuid), mutableMapOf<Any?, Any>())

    return object : BleCentralService {
        override val service: BleServiceDescriptor = service
        override val peripherals: Flow<BlePeripheral>
            get() {
                val peripherals = mutableMapOf<BlePeripheral.Id, DarwinBlePeripheral>()

                return centralDelegate.peripherals
                    .map { discoveredPeripheral ->
                        peripherals.getOrPut(discoveredPeripheral.peripheral.peripheralId) {
                            DarwinBlePeripheral(scope, central, centralDelegate, service, discoveredPeripheral)
                        }.also { it.onDiscoveredPeripheral(discoveredPeripheral) }
                    }
                    .distinct()
                    .shareIn(scope, SharingStarted.Eagerly, Channel.UNLIMITED)
            }
    }
}

private class DarwinBlePeripheral(
    private val scope: CoroutineScope,
    private val central: CBCentralManager,
    private val centralDelegate: CentralDelegate,
    private val service: BleServiceDescriptor,
    discoveredPeripheral: CentralDelegate.DiscoveredPeripheral,
) : BlePeripheral {

    private var connectedCoroutineScope: CoroutineScope? = null

    private val peripheral = discoveredPeripheral.peripheral

    override val peripheralId: BlePeripheral.Id = discoveredPeripheral.peripheral.peripheralId

    private val valueFlows = mutableMapOf<BleUUID, MutableStateFlow<ByteArray?>>()

    override val rssi: MutableStateFlow<BlePeripheral.Rssi> =
        MutableStateFlow(BlePeripheral.Rssi(discoveredPeripheral.rssi))

    override val state: MutableStateFlow<State> =
        MutableStateFlow(if (discoveredPeripheral.isConnectable) State.Disconnected else State.Connectable)

    private fun stateFlowOf(uuid: BleUUID): MutableStateFlow<ByteArray?> {
        return valueFlows.getOrPut(uuid) { MutableStateFlow(null) }
    }

    override fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray> {
        return stateFlowOf(characteristic.uuid).filterNotNull()
    }

    fun onDiscoveredPeripheral(discoveredPeripheral: CentralDelegate.DiscoveredPeripheral) {
        check(peripheral == discoveredPeripheral.peripheral)
        rssi.value = BlePeripheral.Rssi(discoveredPeripheral.rssi)
        state.value = if (discoveredPeripheral.isConnectable) State.Disconnected else State.Connectable
    }

    override fun tryConnect() {
        central.connectPeripheral(peripheral, mutableMapOf<Any?, Any>())

        connectedCoroutineScope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext = scope.coroutineContext + Job(scope.coroutineContext.job)
        }

        connectedCoroutineScope?.launch {
            centralDelegate.connectedPeripherals.first { it == peripheral }
            val peripheralDelegate = PeripheralDelegate(scope, service, onValue = { uuid, value ->
                stateFlowOf(uuid).value = value
            })
            peripheral.delegate = peripheralDelegate
            peripheral.discoverServices(listOf(service.uuid))
        }

        connectedCoroutineScope?.coroutineContext?.job?.invokeOnCompletion {
            central.cancelPeripheralConnection(peripheral)
            peripheral.delegate = null
        }
    }

    override fun tryDisconnect() {
        connectedCoroutineScope?.cancel()
    }
}


private class CentralDelegate(private val scope: CoroutineScope) : NSObject(), CBCentralManagerDelegateProtocol {
    data class DiscoveredPeripheral(
        val peripheral: CBPeripheral,
        val isConnectable: Boolean,
        val rssi: Int
    )

    private val state = MutableStateFlow<Long?>(null)
    private val _peripherals = MutableSharedFlow<DiscoveredPeripheral>()
    val peripherals = _peripherals.asSharedFlow()

    private val _connectedPeripherals = MutableSharedFlow<CBPeripheral>()
    val connectedPeripherals = _connectedPeripherals.asSharedFlow()

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        state.value = central.state
    }

    override fun centralManager(
        central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber
    ) {
        scope.launch {
            _peripherals.emit(
                DiscoveredPeripheral(
                    didDiscoverPeripheral,
                    isConnectable = (advertisementData[CBAdvertisementDataIsConnectable] as? Boolean) ?: true,
                    rssi = RSSI.intValue
                )
            )
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
    private val service: BleServiceDescriptor,
    private val onValue: (uuid: BleUUID, value: ByteArray) -> Unit,
) : NSObject(), CBPeripheralDelegateProtocol {

    private val readCharacteristicChannel = Channel<Unit>()

    @Suppress("UNCHECKED_CAST")
    override fun peripheral(peripheral: CBPeripheral, didDiscoverServices: NSError?) {
        if (didDiscoverServices != null) {
            println("didDiscoverServices error: $didDiscoverServices")
            return
        }
        val cbService = (peripheral.services as List<CBService>).find { it.UUID == service.uuid } ?: return
        peripheral.discoverCharacteristics(service.characteristics.map { it.uuid }, cbService)
    }

    @Suppress("UNCHECKED_CAST", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
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
        onValue(didUpdateValueForCharacteristic.UUID, value.toByteString().toByteArray())
    }
}

private val CBPeripheral.peripheralId: BlePeripheral.Id get() = BlePeripheral.Id(this.identifier.UUIDString)