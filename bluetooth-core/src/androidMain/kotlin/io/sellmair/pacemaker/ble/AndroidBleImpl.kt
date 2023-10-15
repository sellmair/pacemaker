package io.sellmair.pacemaker.ble

import android.content.Context
import io.sellmair.pacemaker.ble.impl.BleCentralServiceImpl
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import io.sellmair.pacemaker.utils.Configuration
import kotlinx.coroutines.*
import java.io.Closeable

context(Configuration)
fun AndroidBle(context: Context): Ble = AndroidBleImpl(context)

context(Configuration)
private class AndroidBleImpl(private val context: Context) : Ble, Closeable {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val coroutineScope = CoroutineScope(Dispatchers.Default.limitedParallelism(1) + SupervisorJob())

    private val queue = BleQueue(coroutineScope.coroutineContext.job)

    override suspend fun createCentralService(service: BleServiceDescriptor): BleCentralService {
        val hardware = AndroidCentralHardware(context)
        val controller: BleCentralController = AndroidCentralController(coroutineScope, hardware, service)
        return BleCentralServiceImpl(coroutineScope, queue, controller, service)
    }

    override suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        val hardware = AndroidPeripheralHardware(context, coroutineScope, service)
        val controller = AndroidPeripheralController(coroutineScope, hardware)
        return BlePeripheralServiceImpl(queue, controller, service, coroutineScope)
    }

    override fun close() {
        coroutineScope.cancel()
    }
}
