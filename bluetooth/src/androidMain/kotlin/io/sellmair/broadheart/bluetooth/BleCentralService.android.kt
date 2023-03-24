@file:Suppress("DEPRECATION", "FunctionName")

package io.sellmair.broadheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.sellmair.broadheart.utils.distinct
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
                .distinct()
    }
}


private class BlePeripheralsContainer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val service: BleServiceDescriptor,
) {
    private val peripheralsById = mutableMapOf<BlePeripheral.Id, AndroidBleBlePeripheral>()

    fun forScanResult(result: ScanResult): BlePeripheral {
        return peripheralsById.getOrPut(result.device.peripheralId) {
            AndroidBleBlePeripheral(context, scope, service, result)
        }.also { it.onScanResult(result) }
    }
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

    override val peripheralId: BlePeripheral.Id = scanResult.device.peripheralId

    private val _rssi = MutableStateFlow(BlePeripheral.Rssi(scanResult.rssi))

    private val device: BluetoothDevice = scanResult.device

    private var gatt: BluetoothGatt? = null

    override val rssi: StateFlow<BlePeripheral.Rssi>
        get() = _rssi.asStateFlow()

    override fun tryConnect() {
        if (gatt == null) {
            this.gatt = device.connectGatt(context, true, this)
        }
    }

    override fun tryDisconnect() {
        if (gatt != null) {
            gatt?.close()
            gatt = null
        }
    }

    fun onScanResult(scanResult: ScanResult) {
        check(this.device == scanResult.device)
        this._rssi.value = BlePeripheral.Rssi(scanResult.rssi)
        this._state.value = if (scanResult.isConnectable) BlePeripheral.State.Connectable
        else BlePeripheral.State.Disconnected
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        connectedCoroutinesScope?.cancel()

        _state.value = when (newState) {
            BluetoothProfile.STATE_DISCONNECTED -> BlePeripheral.State.Connectable
            BluetoothProfile.STATE_CONNECTING -> BlePeripheral.State.Connecting
            BluetoothProfile.STATE_CONNECTED -> BlePeripheral.State.Connected
            else -> state.value
        }

        Log.d("bluetooth", "onConnectionStateChange(status=$status, newState=$newState)")
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            /* Start bluetooth lifecycle */
            parentScope.coroutineContext.job.invokeOnCompletion { gatt.close() }
            gatt.discoverServices()
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

            /* Read out all readable characteristics */
            service.characteristics
                .filter { it.isReadable }
                .forEach { characteristic ->
                    val bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(characteristic.uuid)
                        ?: return@forEach run {
                            Log.d("ble", "Characteristic '${characteristic}' not found in $bluetoothGattService")
                        }

                    valueFlowOf(characteristic.uuid).value = gatt.readCharacteristicValue(bluetoothGattCharacteristic)
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
        setCharacteristicNotification(characteristic, true)
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
