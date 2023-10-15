package io.sellmair.pacemaker.ble

import android.content.Context
import io.sellmair.pacemaker.ble.impl.BleCentralServiceImpl
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.*
import java.io.Closeable

fun AndroidBle(context: Context): Ble = AndroidBleImpl(context)

private class AndroidBleImpl(private val context: Context) : Ble, Closeable {

    override val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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
