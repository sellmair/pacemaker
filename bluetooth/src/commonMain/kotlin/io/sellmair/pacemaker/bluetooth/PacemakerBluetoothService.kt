@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn

interface PacemakerBluetoothService {
    val newConnections: SharedFlow<PacemakerBluetoothConnection>
    val allConnections: SharedFlow<List<PacemakerBluetoothConnection>>
    suspend fun write(write: suspend PacemakerBluetoothWritable.() -> Unit)
}

suspend fun PacemakerBluetoothService(ble: Ble): PacemakerBluetoothService {
    val peripheral = PacemakerPeripheralBluetoothService(ble)
    val central = PacemakerCentralBluetoothService(ble)

    return object : PacemakerBluetoothService {
        override val newConnections: SharedFlow<PacemakerBluetoothConnection> =
            flowOf(peripheral.newConnections, central.newConnections).flattenMerge()
                .shareIn(ble.coroutineScope, SharingStarted.Eagerly)

        override val allConnections: SharedFlow<List<PacemakerBluetoothConnection>> =
            flowOf(peripheral.allConnections, central.allConnections)
                .flattenMerge().shareIn(ble.coroutineScope, SharingStarted.Eagerly)

        override suspend fun write(write: suspend PacemakerBluetoothWritable.() -> Unit) {
            peripheral.write(write)
            central.write(write)
        }
    }
}

