package io.sellmair.pacemaker.ble

import android.content.Context
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.Closeable

internal class AndroidBle(
    private val context: Context
) : Ble, Closeable {

    override val scope = CoroutineScope(Dispatchers.ble + SupervisorJob())

    private val queue = BleQueue(scope)

    override suspend fun createCentralService(service: BleServiceDescriptor): BleCentralService {
        TODO()
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
