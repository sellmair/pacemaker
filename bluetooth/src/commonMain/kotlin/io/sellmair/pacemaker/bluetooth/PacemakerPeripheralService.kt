package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.Ble
import io.sellmair.pacemaker.ble.BlePeripheralService

suspend fun PacemakerPeripheralService(ble: Ble): PacemakerPeripheralService {
    val service = ble.createPeripheralService(PacemakerServiceDescriptors.service)
    return PacemakerPeripheralService(service)
}

class PacemakerPeripheralService(
    private val underlying: BlePeripheralService
) : PacemakerBleWritable by PacemakerBleWritableImpl(underlying) {
    suspend fun startAdvertising() = underlying.startAdvertising()

    val broadcasts = underlying.receivedWrites.receivePacemakerBroadcastPackages()
}


