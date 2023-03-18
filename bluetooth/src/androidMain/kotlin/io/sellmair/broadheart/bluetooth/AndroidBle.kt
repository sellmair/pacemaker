package io.sellmair.broadheart.bluetooth

import android.content.Context
import kotlinx.coroutines.CoroutineScope

class AndroidBle(private val scope: CoroutineScope, private val context: Context) : Ble {
    override suspend fun startServer(service: BleServiceDescriptor): BleServer {
        return AndroidBleServer(scope, context, service)
    }

    override suspend fun startClient(service: BleServiceDescriptor): BleClient {
        return AndroidBleClient(scope, context, service)
    }
}
