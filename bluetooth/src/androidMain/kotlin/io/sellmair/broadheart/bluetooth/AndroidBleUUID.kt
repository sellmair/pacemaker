package io.sellmair.broadheart.bluetooth

import java.util.UUID

actual typealias BleUUID = UUID

actual fun BleUUID(value: String): BleUUID = UUID.fromString(value)

