@file:Suppress("FunctionName")

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleReceivedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlin.coroutines.CoroutineContext

interface PacemakerBluetoothConnection : CoroutineScope {
    val deviceId: BleDeviceId
    val receivedValues: SharedFlow<BleReceivedValue>
}

internal interface WritablePacemakerBluetoothConnection : PacemakerBluetoothConnection, PacemakerBluetoothWritable

internal interface ReadablePacemakerBluetoothConnection {
    suspend fun requestInitialRead()
}

internal fun PacemakerConnection(connection: BleConnection): WritablePacemakerBluetoothConnection =
    object : WritablePacemakerBluetoothConnection,
        ReadablePacemakerBluetoothConnection,
        PacemakerBluetoothWritable by PacemakerBluetoothWritable(connection) {
        override val deviceId: BleDeviceId = connection.deviceId
        override val receivedValues: SharedFlow<BleReceivedValue> = connection.receivedValues
        override val coroutineContext: CoroutineContext = connection.scope.coroutineContext

        override suspend fun requestInitialRead() {
            PacemakerServiceDescriptors.service.characteristics
                .filter { it.isReadable }
                .forEach { connection.requestRead(it) }
        }
    }
