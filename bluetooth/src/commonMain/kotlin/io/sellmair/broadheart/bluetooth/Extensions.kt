package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRateSensorId

fun BlePeripheral.Id.toHeartRateSensorId() = HeartRateSensorId(value)