package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.BleCentralController
import io.sellmair.pacemaker.ble.BleCentralService
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleQueue
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

internal class BleCentralServiceImpl(
    scope: CoroutineScope,
    private val queue: BleQueue,
    private val controller: BleCentralController,
    private val service: BleServiceDescriptor
) : BleCentralService {

    private val connectableById = mutableMapOf<BleDeviceId, BleConnectableImpl>()

    override val connectables: MutableSharedFlow<BleConnectable> = MutableSharedFlow(replay = Channel.UNLIMITED)

    override fun startScanning() {
        controller.startScanning()
    }

    init {
        scope.launch {
            controller.scanResults.consumeAsFlow().collect { result ->
                connectableById.getOrPut(result.deviceId) {
                    BleConnectableImpl(
                        scope, queue, controller.createConnectableController(result), service
                    ).also { newConnectable ->
                        connectables.emit(newConnectable)
                    }
                }.onScanResult(result)
            }
        }
    }
}