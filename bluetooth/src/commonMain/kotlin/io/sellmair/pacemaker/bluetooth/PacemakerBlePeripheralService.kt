package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BlePeripheralService

suspend fun PacemakerBlePeripheralService(ble: Ble): PacemakerBlePeripheralService {
    val service = ble.createPeripheralService(PacemakerServiceDescriptors.service)
    return PacemakerBlePeripheralService(service)
}

class PacemakerBlePeripheralService(
    private val underlying: BlePeripheralService
) : PacemakerBleWritable by PacemakerBleWritableImpl(underlying) {
    suspend fun startAdvertising() = underlying.startAdvertising()
}
