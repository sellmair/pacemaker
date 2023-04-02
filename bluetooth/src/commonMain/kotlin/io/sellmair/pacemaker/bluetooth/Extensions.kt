package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.model.HeartRateSensorId

fun BleDeviceId.toHeartRateSensorId() = HeartRateSensorId(value)