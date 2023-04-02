package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

fun Ble(context: CoroutineContext): Ble = AppleBle(context)

internal class AppleBle(context: CoroutineContext) : Ble {

    private val scope = CoroutineScope(context + Dispatchers.ble + SupervisorJob(context.job))

    private val queue = BleQueue(scope)

    override suspend fun scanForPeripherals(service: BleServiceDescriptor): Flow<BleConnectable> {
        TODO("Not yet implemented")
    }

    override suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        return withContext(scope.coroutineContext) {
            val peripheralHardware = PeripheralHardware(scope, service)
            val peripheralController = AppleBlePeripheralController(scope, peripheralHardware)
            BlePeripheralServiceImpl(queue, peripheralController, service)
        }
    }
}
