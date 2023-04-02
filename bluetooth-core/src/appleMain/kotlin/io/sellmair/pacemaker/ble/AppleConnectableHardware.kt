package io.sellmair.pacemaker.ble

import platform.CoreBluetooth.CBPeripheral

internal class AppleConnectableHardware(
    val peripheral: CBPeripheral,
    val delegate: ApplePeripheralDelegate,
    val serviceDescriptor: BleServiceDescriptor
)