package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRateSensorId

fun BleDeviceId.toHeartRateSensorId() = HeartRateSensorId(value)