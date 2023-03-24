package io.sellmair.broadheart.bluetooth

import kotlinx.coroutines.CoroutineScope

class DarwinBle(override val scope: CoroutineScope) : Ble {
    override suspend fun startPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        return BlePeripheralService(service)
    }

    override suspend fun startCentralService(service: BleServiceDescriptor): BleCentralService {
        return BleCentralService(scope, service)
    }
}