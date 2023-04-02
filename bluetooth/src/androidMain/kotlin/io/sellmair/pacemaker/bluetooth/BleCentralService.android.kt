@file:Suppress("DEPRECATION", "FunctionName")

package io.sellmair.pacemaker.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.sellmair.pacemaker.ble.BleCharacteristicDescriptor
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import io.sellmair.pacemaker.ble.BleUUID
import io.sellmair.pacemaker.utils.distinct
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.coroutines.CoroutineContext

@SuppressLint("MissingPermission")
internal suspend fun BleCentralService(
    scope: CoroutineScope,
    context: Context,
    service: BleServiceDescriptor
): BleCentralService {
    /* Wait for bluetooth permission */
    while (
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        delay(1000)
    }

    val manager = context.getSystemService(BluetoothManager::class.java)
    val peripherals = BlePeripheralsContainer(context, scope, service)
    return object : BleCentralService {
        override val service: BleServiceDescriptor = service

        override val peripherals: Flow<BlePeripheral>
            get() = manager.scanForPeripherals(service)
                .map { scanResult -> peripherals.forScanResult(scanResult) }
                .flowOn(Dispatchers.Main.immediate)
                .distinct()

        override suspend fun setValue(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
            peripherals.all.forEach { peripheral ->
                peripheral.writeValue(characteristic, value)
            }
        }
    }
}

private class BlePeripheralsContainer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: BleServiceDescriptor,
) {
    private val peripheralsById = mutableMapOf<BleDeviceId, AndroidBleBlePeripheral>()

    fun forScanResult(result: ScanResult): BlePeripheral {
        return peripheralsById.getOrPut(result.device.deviceId) {
            println("Ble: ${service.name}: peripheral found: ${result.device.deviceId}")
            AndroidBleBlePeripheral(context, scope, service, result)
        }.also { it.onScanResult(result) }
    }

    val all: List<AndroidBleBlePeripheral> get() = peripheralsById.values.toList()
}


@SuppressLint("MissingPermission")
private class AndroidBleBlePeripheral(
    private val context: Context,
    private val parentScope: CoroutineScope,
    private val service: BleServiceDescriptor,
    scanResult: ScanResult
) : BlePeripheral, BluetoothGattCallback() {

    /* Coroutine scope alive while the bluetooth device is connected */
    private var connectedCoroutinesScope: CoroutineScope? = null

    private val onCharacteristicReadChannel = Channel<CharacteristicReadResult>()

    private val onDescriptorWriteChannel = Channel<Unit>()

    private val _state = MutableStateFlow(
        if (scanResult.isConnectable) BlePeripheral.State.Connectable
        else BlePeripheral.State.Disconnected
    )

    override val state: StateFlow<BlePeripheral.State>
        get() = _state.asStateFlow()

    private val valuesFlows = mutableMapOf<BleUUID, MutableStateFlow<ByteArray?>>()

    private fun valueFlowOf(uuid: BleUUID): MutableStateFlow<ByteArray?> {
        return valuesFlows.getOrPut(uuid) { MutableStateFlow(null) }
    }

    override fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray> {
        return valueFlowOf(characteristic.uuid).filterNotNull()
    }

    private val onCharacteristicWriteChannel = Channel<BluetoothGattCharacteristic>()

    suspend fun writeValue(characteristic: BleCharacteristicDescriptor, value: ByteArray) {
        // if (state.value != BlePeripheral.State.Connected) return
        println("Ble: 'central: writeValue' on ${characteristic.name}")
        val discoveredCharacteristic = discoveredCharacteristics.orEmpty()[characteristic.uuid] ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt?.writeCharacteristic(discoveredCharacteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            onCharacteristicWriteChannel.receiveAsFlow().first { it == discoveredCharacteristic }

        } else {
            discoveredCharacteristic.value = value
            gatt?.writeCharacteristic(discoveredCharacteristic)
        }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        onCharacteristicWriteChannel.trySend(characteristic)
    }

    override val id: BleDeviceId = scanResult.device.deviceId

    private val _rssi = MutableStateFlow(Rssi(scanResult.rssi))

    private var device: BluetoothDevice = scanResult.device

    private var gatt: BluetoothGatt? = null

    private var discoveredCharacteristics: Map<BleUUID, BluetoothGattCharacteristic>? = null

    override val rssi: StateFlow<Rssi>
        get() = _rssi.asStateFlow()

    private var desireConnection = false

    override fun tryConnect() {
        desireConnection = true
    }

    override fun tryDisconnect() {
        if (gatt != null && desireConnection) {
            gatt?.disconnect()
            desireConnection = false
        }
    }

    fun onScanResult(scanResult: ScanResult) {
        check(this.device == scanResult.device)
        this.device = scanResult.device
        this._rssi.value = Rssi(scanResult.rssi)
        this._state.value = if (scanResult.isConnectable) BlePeripheral.State.Connectable
        else BlePeripheral.State.Disconnected

        if (scanResult.isConnectable && desireConnection && gatt == null) {
            gatt = scanResult.device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
        }
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        connectedCoroutinesScope?.cancel()

        _state.value = when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> BlePeripheral.State.Disconnected
            BluetoothProfile.STATE_CONNECTING -> BlePeripheral.State.Connecting
            BluetoothProfile.STATE_CONNECTED -> BlePeripheral.State.Connected
            else -> state.value
        }

        Log.d("Ble", "onConnectionStateChange(gatt=${gatt.device.deviceId}, status=$status, newState=$newState)")
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            /* Start bluetooth lifecycle */
            parentScope.coroutineContext.job.invokeOnCompletion { gatt.close() }
            gatt.discoverServices()
        }

        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            this.gatt?.close()
            this.gatt = null
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        connectedCoroutinesScope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext = Dispatchers.Main + Job(parentScope.coroutineContext.job)
        }

        connectedCoroutinesScope?.launch {
            val bluetoothGattService = gatt.getService(service.uuid) ?: return@launch run {
                Log.d("ble", "Service '$service' not found in gatt $gatt")
            }

            discoveredCharacteristics = service.characteristics.mapNotNull { characteristic ->
                characteristic.uuid to (bluetoothGattService.getCharacteristic(characteristic.uuid)
                    ?: return@mapNotNull null)
            }.toMap()

            /* Read out all readable characteristics */
            service.characteristics
                .filter { it.isReadable }
                .forEach { characteristic ->
                    val bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic.uuid)
                        ?: return@forEach run {
                            Log.d("ble", "Characteristic '${characteristic}' not found in $bluetoothGattService")
                        }

                    val receivedValue = gatt.readCharacteristicValue(bluetoothGattCharacteristic)
                    valueFlowOf(characteristic.uuid).value = receivedValue
                }

            /* Enable notifications */
            service.characteristics
                .filter { it.isNotificationsEnabled }
                .forEach { characteristic ->
                    val bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic.uuid)
                        ?: return@forEach run {
                            Log.d("ble", "Characteristic '${characteristic}' not found in $bluetoothGattService")
                        }

                    gatt.enableNotifications(bluetoothGattCharacteristic)
                }
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int
    ) {
        Log.d("ble", "onCharacteristicRead(status=$status)")

        connectedCoroutinesScope?.launch {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicReadChannel.trySend(CharacteristicReadResult.Success(characteristic, value))
            } else {
                onCharacteristicReadChannel.trySend(CharacteristicReadResult.Failed(characteristic))
                val name = service.characteristics.find { it.uuid == characteristic.uuid }?.name
                Log.d("ble", "Failed reading characteristic for ${name ?: characteristic.uuid} (status: $status)")
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        valueFlowOf(characteristic.uuid).value = value
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        connectedCoroutinesScope?.launch {
            onDescriptorWriteChannel.trySend(Unit)
        }
    }

    private suspend fun BluetoothGatt.readCharacteristicValue(
        characteristic: BluetoothGattCharacteristic
    ): ByteArray? {
        readCharacteristic(characteristic)
        return onCharacteristicReadChannel.receiveAsFlow()
            .filter { it.characteristic == characteristic }
            .first()
            .let { it as? CharacteristicReadResult.Success }
            ?.value
    }

    private suspend fun BluetoothGatt.enableNotifications(characteristic: BluetoothGattCharacteristic) {
        if (!setCharacteristicNotification(characteristic, true)) {
            println("Ble: Failed enabling notifications for '${characteristic.uuid}'")
        }
        val ccdUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val ccdDescriptor: BluetoothGattDescriptor = characteristic.getDescriptor(ccdUUID)
        ccdDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        writeDescriptor(ccdDescriptor)
        onDescriptorWriteChannel.receive()
    }

    sealed interface CharacteristicReadResult {
        val characteristic: BluetoothGattCharacteristic

        class Failed(
            override val characteristic: BluetoothGattCharacteristic,
        ) : CharacteristicReadResult

        class Success(
            override val characteristic: BluetoothGattCharacteristic,
            val value: ByteArray
        ) : CharacteristicReadResult
    }
}
