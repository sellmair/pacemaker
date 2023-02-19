package io.sellmair.broadheart.service

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.coroutines.coroutineContext

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

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun AndroidBroadcastReceiver(context: Context): Flow<GroupMemberState> {
    return callbackFlow {
        while (context.checkSelfPermission(BLUETOOTH_ADVERTISE) != PERMISSION_GRANTED) {
            delay(1000)
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager


        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val data = result.scanRecord?.getServiceData(ParcelUuid(AndroidBroadcaster.serviceUuid))
                if (data != null) {
                    trySendBlocking(Json.decodeFromString<GroupMemberState>(data.decodeToString()))
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
        val serviceUuid = UUID.fromString(serviceUuidString)

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

