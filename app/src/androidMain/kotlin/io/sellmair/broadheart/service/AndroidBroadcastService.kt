package io.sellmair.broadheart.service

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.coroutines.coroutineContext


/*
suspend fun AndroidBroadcaster(context: Context): AndroidBroadcaster {
    /* Wait for bluetooth permission */
    while (context.checkSelfPermission(BLUETOOTH_ADVERTISE) != PERMISSION_GRANTED) {
        delay(1000)
    }

    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val advertiser = manager.adapter.bluetoothLeAdvertiser

    val settings = AdvertisingSetParameters.Builder()
        .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MAX)
        .setConnectable(false)
        .build()

    val data = AdvertiseData.Builder().build()
    val deferredAdvertisingSet = CompletableDeferred<AdvertisingSet>()
    val callback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet, txPower: Int, status: Int) {
            super.onAdvertisingSetStarted(advertisingSet, txPower, status)
            deferredAdvertisingSet.complete(advertisingSet)
        }

    }
    advertiser.startAdvertisingSet(settings, data, null, null, null, callback)

    coroutineContext[Job]?.invokeOnCompletion {
        advertiser.stopAdvertisingSet(callback)
    }

    val advertisingSet = deferredAdvertisingSet.await()

    return AndroidBroadcaster { myState ->
        val encodedState = Json.encodeToString(myState).encodeToByteArray()
        advertisingSet.setAdvertisingData(
            AdvertiseData.Builder()
                .addServiceData(ParcelUuid(AndroidBroadcaster.serviceUuid), encodedState)
                .build()
        )
    }
}

/*
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun AndroidBroadcastReceiver(context: Context): Flow<GroupMemberState> {
    return callbackFlow {
        while (context.checkSelfPermission(BLUETOOTH_ADVERTISE) != PERMISSION_GRANTED
            && context.checkSelfPermission(BLUETOOTH_CONNECT) != PERMISSION_GRANTED
            && context.checkSelfPermission(BLUETOOTH_SCAN) != PERMISSION_GRANTED
        ) {
            delay(1000)
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        val connectedDevices = mutableSetOf<BluetoothDevice>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceParcelUuid = ParcelUuid(AndroidBroadcaster.serviceUuid)
                val record = result.scanRecord ?: return
                if (sericeParcelUuid in record.serviceUuids.orEmpty().toSet()) {
                    if (context.checkSelfPermission(BLUETOOTH_CONNECT) != PERMISSION_GRANTED) return
                    if (!connectedDevices.add(result.device)) return
                    result.device.connectGatt(context, true, object : BluetoothGattCallback() {
                        override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                            super.onPhyUpdate(gatt, txPhy, rxPhy, status)
                        }

                        override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
                            super.onPhyRead(gatt, txPhy, rxPhy, status)
                        }

                        @SuppressLint("MissingPermission")
                        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                                gatt.discoverServices()
                            }
                            super.onConnectionStateChange(gatt, status, newState)
                        }

                        @SuppressLint("MissingPermission")
                        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                            super.onServicesDiscovered(gatt, status)
                            val char = gatt.getService(serviceParcelUuid.uuid)
                                ?.getCharacteristic(AndroidBroadcaster.characteristicUuid) ?: return

                            val enabledNotifications = gatt.setCharacteristicNotification(char, true)

                            val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            val descriptor: BluetoothGattDescriptor = char.getDescriptor(uuid)
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                            gatt.writeDescriptor(descriptor)

                            val read = gatt.readCharacteristic(char)

                            println("enabledNotifications=$enabledNotifications | read=$read")

                        }

                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray,
                            status: Int
                        ) {
                            super.onCharacteristicRead(gatt, characteristic, value, status)
                            if(status == BluetoothGatt.GATT_SUCCESS) {
                                trySend(Json.decodeFromString<GroupMemberState>(value.decodeToString()))
                            }
                        }

                        override fun onCharacteristicWrite(
                            gatt: BluetoothGatt?,
                            characteristic: BluetoothGattCharacteristic?,
                            status: Int
                        ) {
                            super.onCharacteristicWrite(gatt, characteristic, status)
                        }

                        override fun onCharacteristicChanged(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray
                        ) {
                            super.onCharacteristicChanged(gatt, characteristic, value)
                        }

                        override fun onDescriptorWrite(
                            gatt: BluetoothGatt?,
                            descriptor: BluetoothGattDescriptor?,
                            status: Int
                        ) {
                            super.onDescriptorWrite(gatt, descriptor, status)
                        }

                        override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
                            super.onReliableWriteCompleted(gatt, status)
                        }

                        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
                            super.onReadRemoteRssi(gatt, rssi, status)
                        }

                        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
                            super.onMtuChanged(gatt, mtu, status)
                        }

                        override fun onServiceChanged(gatt: BluetoothGatt) {
                            super.onServiceChanged(gatt)
                        }
                    }, BluetoothDevice.TRANSPORT_LE)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                super.onBatchScanResults(results)
            }
        }
        manager.adapter.bluetoothLeScanner.startScan(callback)

        awaitClose {
            manager.adapter.bluetoothLeScanner.stopScan(callback)
        }
    }
}

fun interface AndroidBroadcaster {
    companion object {
        const val serviceUuidString = "35b6d6ed-b85f-48e6-8f21-26c9877dbbe8"
        const val characteristicUuidString = "c76b2e24-a272-4a7f-bd66-7acad9bd0014"
        val serviceUuid = UUID.fromString(serviceUuidString)
        val characteristicUuid = UUID.fromString(characteristicUuidString)

    }

    suspend fun setMyState(state: GroupMemberState)
}

class AndroidBroadcastService(
    private val broadcaster: AndroidBroadcaster
) {
    suspend fun broadcastMyState(state: GroupMemberState) {
        broadcaster.setMyState(state)
    }
}
 */
 */