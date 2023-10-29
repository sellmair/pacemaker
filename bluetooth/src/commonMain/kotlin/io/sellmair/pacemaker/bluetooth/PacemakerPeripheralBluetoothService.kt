package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleReceivedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

internal interface PacemakerPeripheralBluetoothService : PacemakerBluetoothService

internal suspend fun PacemakerPeripheralBluetoothService(ble: Ble): PacemakerPeripheralBluetoothService {
    val service = ble.createPeripheralService(PacemakerServiceDescriptors.service)

    val connectionsById = mutableMapOf<BleDeviceId, PeripheralPacemakerBluetoothConnection>()
    val newConnections = MutableSharedFlow<PeripheralPacemakerBluetoothConnection>()
    val allConnections = MutableStateFlow<List<PeripheralPacemakerBluetoothConnection>>(emptyList())

    suspend fun createNewConnection(id: BleDeviceId): PeripheralPacemakerBluetoothConnection {
        val connection = PeripheralPacemakerBluetoothConnection(id, ble.coroutineScope)
        connectionsById[id] = connection
        connection.coroutineContext.job.invokeOnCompletion {
            connectionsById.remove(id)
            allConnections.value -= connection
        }
        allConnections.value += connection
        newConnections.emit(connection)
        return connection
    }

    suspend fun onReceivedValue(value: BleReceivedValue) {
        val connection = connectionsById[value.deviceId] ?: createNewConnection(value.deviceId)
        connection.startConnectionTimeout()
        connection.receivedValues.emit(value)
    }

    ble.coroutineScope.launch {
        service.receivedWrites.collect(::onReceivedValue)
    }

    service.startAdvertising()

    return object : PacemakerPeripheralBluetoothService {
        override val newConnections: SharedFlow<PacemakerBluetoothConnection> = newConnections
        override val allConnections: SharedFlow<List<PacemakerBluetoothConnection>> = allConnections

        override suspend fun write(write: suspend PacemakerBluetoothWritable.() -> Unit) {
             PacemakerBluetoothWritable(service).write()
        }
    }
}

private class PeripheralPacemakerBluetoothConnection(
    override val deviceId: BleDeviceId,
    parentScope: CoroutineScope
) : PacemakerBluetoothConnection {
    override val receivedValues: MutableSharedFlow<BleReceivedValue> = MutableSharedFlow()

    override val coroutineContext: CoroutineContext =
        parentScope.coroutineContext + Job(parentScope.coroutineContext.job)

    private var timeoutConnectionJob: Job? = null

    fun startConnectionTimeout() {
        timeoutConnectionJob?.cancel()
        timeoutConnectionJob = launch {
            delay(1.minutes)
            this@PeripheralPacemakerBluetoothConnection.cancel()
        }
    }
}
