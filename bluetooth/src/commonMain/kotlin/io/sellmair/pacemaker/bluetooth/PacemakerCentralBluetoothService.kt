@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.bluetooth.PacemakerCentralBluetoothService.Companion.log
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal interface PacemakerCentralBluetoothService : PacemakerBluetoothService {
    companion object {
        val log = LogTag.ble.forClass<PacemakerCentralBluetoothService>()
    }
}

internal suspend fun PacemakerCentralBluetoothService(ble: Ble): PacemakerCentralBluetoothService {
    val service = ble.createCentralService(PacemakerServiceDescriptors.service)
    val allConnections = MutableStateFlow<List<WritablePacemakerBluetoothConnection>>(emptyList())

    fun onConnection(connection: BleConnection) {
        /* Request all readable characteristics in loop */
        connection.scope.launch {
            while (true) {
                PacemakerServiceDescriptors.service.characteristics
                    .filter { it.isReadable }
                    .forEach { readableCharacteristic -> connection.requestRead(readableCharacteristic) }
                delay(15.seconds)
            }
        }

        /* Enable notifications for all characteristics that support it */
        connection.scope.launch {
            PacemakerServiceDescriptors.service.characteristics
                .filter { it.isNotificationsEnabled }
                .forEach { characteristic -> connection.enableNotifications(characteristic) }
        }

        /* Logging: Log all received values */
        connection.scope.launch {
            connection.receivedValues.collect { log.debug("${it.deviceId}: received ${it.characteristic.name}") }
        }
    }

    fun onPacemakerConnection(connection: WritablePacemakerBluetoothConnection) {
        allConnections.value += connection
        connection.coroutineContext.job.invokeOnCompletion {
            allConnections.value -= connection
        }
    }

    val newConnections = service
        .connectables
        .onEach { it.connectIfPossible(true) }
        .flatMapMerge { it.connection }
        .onEach(::onConnection)
        .map(::PacemakerConnection)
        .onEach(::onPacemakerConnection)
        .shareIn(ble.coroutineScope, SharingStarted.Eagerly)

    service.startScanning()

    return object : PacemakerCentralBluetoothService {
        override val newConnections: SharedFlow<PacemakerBluetoothConnection> = newConnections
        override val allConnections: StateFlow<List<PacemakerBluetoothConnection>> = allConnections
        override fun write(write: suspend PacemakerBluetoothWritable.() -> Unit) =
            allConnections.value.forEach { connection ->
                connection.launch { connection.write() }
            }
    }
}
