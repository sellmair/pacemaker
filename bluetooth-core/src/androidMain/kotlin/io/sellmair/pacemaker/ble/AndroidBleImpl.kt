package io.sellmair.pacemaker.ble

import android.Manifest.permission.*
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.getSystemService
import io.sellmair.pacemaker.ble.impl.BleCentralServiceImpl
import io.sellmair.pacemaker.ble.impl.BlePeripheralServiceImpl
import kotlinx.coroutines.*
import java.io.Closeable

suspend fun AndroidBle(context: Context): Ble? {
    val manager = context.getSystemService<BluetoothManager>() ?: return null

    /* Await permissions */
    while (
        context.checkSelfPermission(BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
    ) {
        delay(250)
    }

    /* Await Bluetooth adapter to be turned on */
    while(
        manager.adapter?.isEnabled != true
    ) {
        delay(1000)
    }

    return AndroidBleImpl(context)
}


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
