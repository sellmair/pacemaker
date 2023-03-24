package io.sellmair.broadheart.bluetooth

import android.bluetooth.BluetoothDevice

internal val BluetoothDevice.peripheralId: BlePeripheral.Id get() = BlePeripheral.Id(address)
