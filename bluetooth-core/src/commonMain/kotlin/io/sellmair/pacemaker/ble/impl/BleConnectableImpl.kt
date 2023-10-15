package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.*
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Connected
import io.sellmair.pacemaker.ble.BleConnectable.ConnectionState.Disconnected
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.info
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

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
                if (controller.isConnected.value) return@connect BleSuccess()
                controller.connect()
            }
        }
    }

    private suspend fun createNewConnection(): BleConnection? {
        queue.enqueue(DiscoverServicesBleOperation(controller.deviceId, controller::discoverService))
            .getOr { return null }

        queue.enqueue(DiscoverCharacteristicsBleOperation(controller.deviceId, controller::discoverCharacteristics))
            .getOr { return null }

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