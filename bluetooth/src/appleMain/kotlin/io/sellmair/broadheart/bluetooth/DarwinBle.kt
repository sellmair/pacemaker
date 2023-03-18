package io.sellmair.broadheart.bluetooth

import kotlinx.coroutines.CoroutineScope

class DarwinBle(private val scope: CoroutineScope) : Ble {
    override suspend fun startServer(service: BleServiceDescriptor): BleServer {
        return DarwinBleServer(service)
    }

    override suspend fun startClient(service: BleServiceDescriptor): BleClient {
        return DarwinBleClient(scope, service)
    }
}