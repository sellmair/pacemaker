@file:Suppress("DEPRECATION", "FunctionName")

package io.sellmair.broadheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import io.sellmair.broadheart.utils.distinct
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.coroutines.CoroutineContext

@SuppressLint("MissingPermission")
internal suspend fun AndroidBleClient(
    scope: CoroutineScope,
    context: Context,
    service: BleServiceDescriptor
): BleClient {
    /* Wait for bluetooth permission */
    while (
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        delay(1000)
    }

    val manager = context.getSystemService(BluetoothManager::class.java)
    return object : BleClient {
        override val service: BleServiceDescriptor = service
        override val peripherals: Flow<BleDiscoveredPeripheral>
            get() = manager.scanForPeripherals(service)
                .distinct { scanResult -> scanResult.device.peripheralId }
                .map { scanResult ->
                    object : BleDiscoveredPeripheral {
                        override val rssi: Int get() = scanResult.rssi
                        override val peripheralId: BlePeripheralId = scanResult.device.peripheralId
                        override suspend fun connect(): BleClientConnection {
                            return scanResult.device.connect(scope, context, service)
                        }
                    }
                }
    }
}


private val BluetoothDevice.peripheralId: BlePeripheralId get() = BlePeripheralId(address)

@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
private fun BluetoothManager.scanForPeripherals(service: BleServiceDescriptor): Flow<ScanResult> = callbackFlow {
    val scanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("bluetooth", "onScanFailed(errorCode=$errorCode)")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result == null) return
            trySend(result)
        }
    }

    val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(service.uuid))
        .build()

    val scanSettings = ScanSettings.Builder()
        .setLegacy(false)
        .build()

    adapter.bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
    awaitClose { adapter.bluetoothLeScanner.stopScan(scanCallback) }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun BluetoothDevice.connect(
    scope: CoroutineScope, context: Context, service: BleServiceDescriptor
): AndroidBleClientConnection {
    val connectedBluetoothDevice = AndroidBleClientConnection(scope, service, peripheralId)
    connectGatt(context, true, connectedBluetoothDevice, BluetoothDevice.TRANSPORT_LE)
    return connectedBluetoothDevice
}

@SuppressLint("MissingPermission")
private class AndroidBleClientConnection(
    private val parentScope: CoroutineScope,
    private val service: BleServiceDescriptor,
    override val peripheralId: BlePeripheralId
) : BleClientConnection, BluetoothGattCallback() {

    /* Coroutine scope alive while the bluetooth device is connected */
    private var connectedCoroutinesScope: CoroutineScope? = null

    private val onCharacteristicReadChannel = Channel<CharacteristicReadResult>()
    private val onDescriptorWriteChannel = Channel<Unit>()

    private val valuesFlows = mutableMapOf<BleUUID, MutableStateFlow<ByteArray?>>()

    private fun valueFlowOf(uuid: BleUUID): MutableStateFlow<ByteArray?> {
        return valuesFlows.getOrPut(uuid) { MutableStateFlow(null) }
    }

    override fun getValue(characteristic: BleCharacteristicDescriptor): Flow<ByteArray> {
        return valueFlowOf(characteristic.uuid).filterNotNull()
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        connectedCoroutinesScope?.cancel()
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
