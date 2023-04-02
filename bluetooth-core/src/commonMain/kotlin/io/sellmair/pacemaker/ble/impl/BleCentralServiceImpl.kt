package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.*
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

    private val connectablesById = mutableMapOf<BleDeviceId, BleConnectableImpl>()

    override val connectables: MutableSharedFlow<BleConnectable> = MutableSharedFlow(replay = Channel.UNLIMITED)

    override fun startScanning() {
        controller.startScanning()
    }

    init {
        scope.launch {
            controller.scanResults.consumeAsFlow().collect { result ->
                connectablesById.getOrPut(result.deviceId) {
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