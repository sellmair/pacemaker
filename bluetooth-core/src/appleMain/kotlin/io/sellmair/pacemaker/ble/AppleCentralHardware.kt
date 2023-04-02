package io.sellmair.pacemaker.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerStatePoweredOn

internal class AppleCentralHardware(
    val manager: CBCentralManager,
    val delegate: AppleCentralManagerDelegate,
    val serviceDescriptor: BleServiceDescriptor
)

internal suspend fun AppleCentralHardware(
    scope: CoroutineScope,
    serviceDescriptor: BleServiceDescriptor
): AppleCentralHardware {
    val delegate = AppleCentralManagerDelegate(scope)
    val manager = CBCentralManager(delegate, null)
    delegate.state.first { it == CBCentralManagerStatePoweredOn }
    return AppleCentralHardware(
        manager = manager,
        delegate = delegate,
        serviceDescriptor = serviceDescriptor
    )
}
