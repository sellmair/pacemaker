@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import kotlinx.coroutines.flow.*

interface PacemakerBluetoothService {
    val newConnections: SharedFlow<PacemakerBluetoothConnection>
    val allConnections: SharedFlow<List<PacemakerBluetoothConnection>>
    fun write(write: suspend PacemakerBluetoothWritable.() -> Unit)
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

        override fun write(write: suspend PacemakerBluetoothWritable.() -> Unit) {
            peripheral.write(write)
            central.write(write)
        }
    }
}

