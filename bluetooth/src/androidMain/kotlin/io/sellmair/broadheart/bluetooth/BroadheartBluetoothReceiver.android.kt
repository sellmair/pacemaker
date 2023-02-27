@file:OptIn(FlowPreview::class)

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
import io.sellmair.broadheart.bluetooth.ServiceConstants.heartRateCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.heartRateLimitCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.sensorIdCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.userIdCharacteristicUuidString
import io.sellmair.broadheart.bluetooth.ServiceConstants.userNameCharacteristicUuidString
import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.HeartRateSensorId
import io.sellmair.broadheart.model.UserId
import io.sellmair.broadheart.utils.decodeToInt
import io.sellmair.broadheart.utils.distinct
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.TimeSource

suspend fun BroadheartBluetoothReceiver(
    context: Context, scope: CoroutineScope
): BroadheartBluetoothReceiver {
    /* Wait for bluetooth permission */
    while (
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        delay(1000)
    }

    val receivedBroadheartPackages = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
        .scanForBroadheartPeripherals()
        .distinct { device -> device.address }
        .map { device -> device.connect(context, currentCoroutineContext().job) }
        .flatMapMerge { connection ->
            val address = connection.address
            var userId: UserId? = null
            var sensorId: HeartRateSensorId? = null
            var userName: String? = null
            var heartRate: HeartRate? = null
            var heartRateLimit: HeartRate? = null

            channelFlow {
                suspend fun emitIfPossible() {
                    send(
                        ReceivedBroadheartPackage(
                            receivedTime = TimeSource.Monotonic.markNow(),
                            address = address,
                            userId = userId ?: return,
                            sensorId = sensorId ?: return,
                            userName = userName ?: return,
                            heartRate = heartRate ?: return,
                            heartRateLimit = heartRateLimit ?: return
                        )
                    )
                }

                coroutineScope {
                    launch {
                        connection.userId().collect {
                            userId = it
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.sensorId().collect {
                            sensorId = it
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.userName().collect {
                            userName = it
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.heartRate().collect {
                            heartRate = it
                            emitIfPossible()
                        }
                    }

                    launch {
                        connection.heartRateLimit().collect {
                            heartRateLimit = it
                            emitIfPossible()
                        }
                    }
                }
            }
        }

    return object : BroadheartBluetoothReceiver {
        override val received: SharedFlow<ReceivedBroadheartPackage> = receivedBroadheartPackages
            .shareIn(scope, SharingStarted.Eagerly)
    }
}


@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
private fun BluetoothManager.scanForBroadheartPeripherals(): Flow<BluetoothDevice> = callbackFlow {
    val scanCallback = object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("bluetooth", "onScanFailed(errorCode=$errorCode)")
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result == null) return
            trySend(result.device)
        }
    }

    val scanFilter = ScanFilter.Builder()
        .setServiceUuid(ParcelUuid(UUID.fromString(ServiceConstants.serviceUuidString)))
        .build()

    val scanSettings = ScanSettings.Builder()
        .setLegacy(false)
        .build()

    adapter.bluetoothLeScanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
    awaitClose { adapter.bluetoothLeScanner.stopScan(scanCallback) }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun BluetoothDevice.connect(context: Context, parentJob: Job): BroadheartBluetoothConnection {
    val connectedBluetoothDevice = BroadheartBluetoothConnection(this, parentJob)
    connectGatt(context, true, connectedBluetoothDevice, BluetoothDevice.TRANSPORT_LE)
    return connectedBluetoothDevice
}

@SuppressLint("MissingPermission")
private class BroadheartBluetoothConnection(
    private val device: BluetoothDevice,
    private val parentJob: Job?
) : BluetoothGattCallback() {

    /* Coroutine scope alive while the bluetooth device is connected */
    private var connectedCoroutinesScope: CoroutineScope? = null

    private val userId = MutableStateFlow<Long?>(null)
    private val userName = MutableStateFlow<String?>(null)
    private val heartRate = MutableStateFlow<HeartRate?>(null)
    private val heartRateLimit = MutableStateFlow<HeartRate?>(null)
    private val sensorId = MutableStateFlow<String?>(null)

    private val onCharacteristicReadChannel = Channel<CharacteristicReadResult>()
    private val onDescriptorWriteChannel = Channel<Unit>()

    val address: String get() = device.address
    fun userId(): Flow<UserId> = userId.filterNotNull().map(::UserId)
    fun userName(): Flow<String> = userName.filterNotNull()
    fun sensorId(): Flow<HeartRateSensorId> = sensorId.filterNotNull().map(::HeartRateSensorId)
    fun heartRate(): Flow<HeartRate> = heartRate.filterNotNull()
    fun heartRateLimit(): Flow<HeartRate> = heartRateLimit.filterNotNull()

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        connectedCoroutinesScope?.cancel()
        Log.d("bluetooth", "onConnectionStateChange(status=$status, newState=$newState)")
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            /* Start bluetooth lifecycle */
            parentJob?.invokeOnCompletion { gatt.close() }
            gatt.discoverServices()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d("bluetooth", "onServicesDiscovered(status=$status)")

        connectedCoroutinesScope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext = Dispatchers.Main + Job(parentJob)
        }

        /* Read values and enable notifications for heart rate, heart rate limit and sensorId */
        connectedCoroutinesScope?.launch {
            val service = gatt.getService(UUID.fromString(ServiceConstants.serviceUuidString)) ?: return@launch

            val sensorIdCharacteristic = service.getCharacteristic(UUID.fromString(sensorIdCharacteristicUuidString))
                ?: return@launch

            val heartRateCharacteristic = service.getCharacteristic(
                UUID.fromString(heartRateCharacteristicUuidString)
            ) ?: return@launch

            val heartRateLimitCharacteristic = service.getCharacteristic(
                UUID.fromString(heartRateLimitCharacteristicUuidString)
            ) ?: return@launch


            val userIdCharacteristic = service.getCharacteristic(UUID.fromString(userIdCharacteristicUuidString))
                ?: return@launch

            val userNameCharacteristic = service.getCharacteristic(UUID.fromString(userNameCharacteristicUuidString))
                ?: return@launch

            userId.value = ByteBuffer.wrap(gatt.readValue(userIdCharacteristic) ?: return@launch).getLong()
            userName.value = gatt.readValue(userNameCharacteristic)?.decodeToString()
            sensorId.value = gatt.readValue(sensorIdCharacteristic)?.decodeToString()
            heartRate.value = gatt.readValue(heartRateCharacteristic)?.decodeToInt()?.let(::HeartRate)
            heartRateLimit.value = gatt.readValue(heartRateLimitCharacteristic)?.decodeToInt()?.let(::HeartRate)

            gatt.enableNotifications(sensorIdCharacteristic)
            gatt.enableNotifications(heartRateCharacteristic)
            gatt.enableNotifications(heartRateLimitCharacteristic)
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
        Log.d("bluetooth", "onCharacteristicRead(status=$status)")
        connectedCoroutinesScope?.launch {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                onCharacteristicReadChannel.trySend(CharacteristicReadResult.Success(characteristic, value))
            } else {
                onCharacteristicReadChannel.trySend(CharacteristicReadResult.Failed(characteristic))
            }
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        super.onCharacteristicChanged(gatt, characteristic, value)
        when (characteristic.uuid) {
            UUID.fromString(sensorIdCharacteristicUuidString) ->
                sensorId.value = value.decodeToString()

            UUID.fromString(heartRateCharacteristicUuidString) ->
                heartRate.value = HeartRate(value.decodeToInt())

            UUID.fromString(heartRateLimitCharacteristicUuidString) ->
                heartRateLimit.value = HeartRate(value.decodeToInt())
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        connectedCoroutinesScope?.launch {
            onDescriptorWriteChannel.trySend(Unit)
        }
    }

    private suspend fun BluetoothGatt.readValue(characteristic: BluetoothGattCharacteristic): ByteArray? {
        readCharacteristic(characteristic)
        return onCharacteristicReadChannel.receiveAsFlow()
            .filter { it.characteristic == characteristic }
            .first()
            .let { it as? CharacteristicReadResult.Success }
            ?.value
    }

    @Suppress("DEPRECATION")
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

