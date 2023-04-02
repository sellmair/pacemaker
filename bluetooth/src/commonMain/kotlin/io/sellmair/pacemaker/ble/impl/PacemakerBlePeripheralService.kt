package io.sellmair.pacemaker.ble.impl

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BlePeripheralService
import io.sellmair.pacemaker.ble.PacemakerBleWritable
import io.sellmair.pacemaker.bluetooth.PacemakerBleService

suspend fun Ble.createPacemakerBlePeripheralService(): PacemakerBlePeripheralService {
    val service = createPeripheralService(PacemakerBleService.service)
    return PacemakerBlePeripheralService(service)
}

class PacemakerBlePeripheralService(
    private val underlying: BlePeripheralService
) : PacemakerBleWritable by PacemakerBleWritableImpl(underlying) {
    suspend fun startAdvertising() = underlying.startAdvertising()
}
