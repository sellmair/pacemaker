@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BleCentralService
import io.sellmair.pacemaker.ble.BleConnection
import io.sellmair.pacemaker.ble.ble
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

suspend fun PacemakerCentralService(ble: Ble): PacemakerCentralService {
    val service = ble.createCentralService(PacemakerServiceDescriptors.service)
    return PacemakerCentralService(ble.scope, service)
}

class PacemakerCentralService(
    private val scope: CoroutineScope,
    private val underlying: BleCentralService
) {

    private val service = PacemakerServiceDescriptors.service

    private val _currentConnections = mutableListOf<BleConnection>()

    suspend fun currentConnections() = withContext(scope.coroutineContext) {
        _currentConnections.toList()
    }

    val connections = underlying
        .connectables
        .onEach { it.connectIfPossible(true) }
        .flatMapMerge { it.connection }
        .onEach { onConnection(it) }
        .map { it }
        .shareIn(scope, SharingStarted.Eagerly)

    private fun onConnection(connection: BleConnection) {
        _currentConnections.add(connection)
        connection.scope.coroutineContext.job.invokeOnCompletion {
            _currentConnections.remove(connection)
        }

        /* Request all readable characteristics in loop */
        connection.scope.launch {
            while (true) {
                service.characteristics.filter { it.isReadable }.forEach { readableCharacteristic ->
                    connection.requestRead(readableCharacteristic)
                }
                delay(15.seconds)
            }
        }

        connection.scope.launch {
            service.characteristics.filter { it.isNotificationsEnabled }.forEach { characteristic ->
                connection.enableNotifications(characteristic)
            }
        }

        connection.receivedValues
            .onEach { log.debug("${it.deviceId}: received ${it.characteristic.name}") }
            .launchIn(connection.scope)
    }

    init {
        underlying.startScanning()
    }

    companion object {
        val log = LogTag.ble.forClass<PacemakerCentralService>()
    }
}