package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.ble.ApplePeripheralHardware.Companion.log
import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import platform.CoreBluetooth.CBMutableService
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerStatePoweredOn

internal class ApplePeripheralHardware(
    val manager: CBPeripheralManager,
    val delegate: ApplePeripheralManagerDelegate,
    val service: CBMutableService,
    val serviceDescriptor: BleServiceDescriptor
) {
    companion object {
        val log = LogTag.ble.forClass<ApplePeripheralHardware>()
    }
}

internal suspend fun ApplePeripheralHardware(
    scope: CoroutineScope,
    serviceDescriptor: BleServiceDescriptor
): ApplePeripheralHardware {
    val managerDelegate = ApplePeripheralManagerDelegate(scope)
    val manager = CBPeripheralManager(managerDelegate, null)
    /* Wait for bluetooth to power on */
    managerDelegate.state.first { it == CBPeripheralManagerStatePoweredOn }

    val service = CBMutableService(serviceDescriptor)
    manager.addService(service)
    managerDelegate.didAddService.first { it.service == service }.error?.let { error ->
        log.error("Failed adding service $serviceDescriptor: ${error.localizedDescription}")
    }

    return ApplePeripheralHardware(
        manager = manager, delegate = managerDelegate, service = service, serviceDescriptor = serviceDescriptor
    )
}