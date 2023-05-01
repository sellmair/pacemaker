package io.sellmair.pacemaker.ble

import android.content.Context
import io.sellmair.pacemaker.ble.impl.BleCentralServiceImpl
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.Closeable

fun AndroidBle(context: Context): Ble = AndroidBleImpl(context)

private class AndroidBleImpl(private val context: Context) : Ble, Closeable {

    override val scope = CoroutineScope(Dispatchers.ble + SupervisorJob())

    private val queue = BleQueue(scope)

    override suspend fun createCentralService(service: BleServiceDescriptor): BleCentralService {
        val hardware = AndroidCentralHardware(context)
        val controller: BleCentralController = AndroidCentralController(scope, hardware, service)
        return BleCentralServiceImpl(scope, queue, controller, service)
    }

    override suspend fun createPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        val hardware = AndroidPeripheralHardware(context, scope, service)
        val controller = AndroidPeripheralController(scope, hardware)
        return BlePeripheralServiceImpl(queue, controller, service)
    }

    override fun close() {
        scope.cancel()
    }
}
