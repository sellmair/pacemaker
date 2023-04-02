package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.impl.BleCentralServiceImpl
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.*

fun Ble(): Ble = AppleBle()

internal class AppleBle : Ble {

    override val scope = CoroutineScope(Dispatchers.ble + SupervisorJob())

    private val queue = BleQueue(scope)

    override suspend fun createCentralService(service: BleServiceDescriptor): BleCentralService {
        return withContext(scope.coroutineContext) {
            val centralHardware = AppleCentralHardware(scope, service)
            val controller = AppleCentralController(scope, centralHardware)
            BleCentralServiceImpl(scope, queue, controller, service)
        }
    }

    override suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        return withContext(scope.coroutineContext) {
            val peripheralHardware = ApplePeripheralHardware(scope, service)
            val peripheralController = ApplePeripheralController(scope, peripheralHardware)
            BlePeripheralServiceImpl(queue, peripheralController, service)
        }
    }

    override fun close() {
        scope.cancel()
    }
}


