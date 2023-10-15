package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.impl.BleCentralServiceImpl
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.*

fun AppleBle(): Ble = AppleBleImpl()

private class AppleBleImpl : Ble {

    override val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val queue = BleQueue(coroutineScope.coroutineContext.job)

    override suspend fun createCentralService(service: BleServiceDescriptor): BleCentralService {
        return withContext(coroutineScope.coroutineContext) {
            val centralHardware = AppleCentralHardware(coroutineScope, service)
            val controller = AppleCentralController(coroutineScope, centralHardware)
            BleCentralServiceImpl(coroutineScope, queue, controller, service)
        }
    }

    override suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        return withContext(coroutineScope.coroutineContext) {
            val peripheralHardware = ApplePeripheralHardware(coroutineScope, service)
            val peripheralController = ApplePeripheralController(coroutineScope, peripheralHardware)
            BlePeripheralServiceImpl(queue, peripheralController, service, coroutineScope)
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }
}


