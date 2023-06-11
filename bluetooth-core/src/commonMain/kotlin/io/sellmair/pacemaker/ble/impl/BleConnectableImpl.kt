package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.BleCentralController
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connected
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Disconnected
import io.sellmair.pacemaker.ble.BleConnectableController
import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleQueue
import io.sellmair.pacemaker.ble.BleServiceDescriptor
import io.sellmair.pacemaker.ble.BleSimpleResult
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.ble.invokeOnFailure
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

internal class BleConnectableImpl(
    private val scope: CoroutineScope,
    private val queue: BleQueue,
    private val controller: BleConnectableController,
    override val service: BleServiceDescriptor,
) : BleConnectable {

    override val deviceName: String? = controller.deviceName

    override val deviceId: BleDeviceId = controller.deviceId

    override val connectionState = MutableStateFlow(Disconnected)

    override val connectIfPossible = MutableStateFlow(false)

    private val _connection = MutableStateFlow<BleConnection?>(null)

    override val connection: SharedFlow<BleConnection> = _connection
        .filterNotNull()
        .shareIn(scope, SharingStarted.Eagerly)


    private val _rssi = MutableStateFlow<Int?>(null)

    override val rssi = _rssi.asStateFlow()

    override fun connectIfPossible(connect: Boolean) {
        connectIfPossible.value = connect
        if (!connect) {
            scope.launch {
                queue enqueue DisconnectPeripheralBleOperation(controller.deviceId) disconnect@{
                    controller.disconnect()
                }
            }
        }
    }

    suspend fun onScanResult(result: BleCentralController.ScanResult) {
        _rssi.value = result.rssi
        if (connectIfPossible.value && connectionState.value == Disconnected && result.isConnectable) {
            connectionState.value = BleConnectable.ConnectionState.Connecting
            queue enqueue ConnectPeripheralBleOperation(controller.deviceId) connect@{
                if (controller.isConnected.value) return@connect BleSimpleResult.Success
                controller.connect()
            }
        }
    }

    private suspend fun createNewConnection(): BleConnection? {
        queue.enqueue(DiscoverServicesBleOperation(controller.deviceId, controller::discoverService))
            .invokeOnFailure { return null }

        queue.enqueue(DiscoverCharacteristicsBleOperation(controller.deviceId, controller::discoverCharacteristics))
            .invokeOnFailure { return null }

        val connectionScope = CoroutineScope(scope.coroutineContext + Job(scope.coroutineContext.job))
        return BleConnectionImpl(connectionScope, queue, controller, service)
    }

    init {
        controller.isConnected
            /* Maintain isConnected flow */
            .onEach { isConnected -> connectionState.value = if (isConnected) Connected else Disconnected }

            /* Maintain active connection */
            .onEach { isConnected ->
                _connection.value?.scope?.cancel()
                _connection.value = null
                if (isConnected) {
                    _connection.value = createNewConnection()
                }
            }
            .launchIn(scope)

        /* Log current connection state */
        connectionState
            .onEach { state -> log.info("'${controller.deviceId}': $state") }
            .launchIn(scope)
    }

    companion object {
        val log = LogTag.ble.forClass<BleConnectable>()
    }
}