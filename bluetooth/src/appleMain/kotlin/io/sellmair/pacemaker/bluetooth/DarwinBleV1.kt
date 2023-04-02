package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleServiceDescriptor
import kotlinx.coroutines.CoroutineScope

class DarwinBleV1(override val scope: CoroutineScope) : BleV1 {
    override suspend fun startPeripheralService(service: BleServiceDescriptor): BlePeripheralService {
        return BlePeripheralService(service)
    }

    override suspend fun startCentralService(service: BleServiceDescriptor): BleCentralService {
        return BleCentralService(scope, service)
    }
}