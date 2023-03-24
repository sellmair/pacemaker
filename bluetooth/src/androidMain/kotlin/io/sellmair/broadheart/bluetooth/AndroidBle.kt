package io.sellmair.broadheart.bluetooth

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class AndroidBle(override val scope: CoroutineScope, private val context: Context) : Ble {
    override suspend fun startPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        return BlePeripheralService(scope, context, service)
    }

    override suspend fun startCentralService(service: BleServiceDescriptor): BleCentralService {
        return BleCentralService(scope, context, service)
    }
}
