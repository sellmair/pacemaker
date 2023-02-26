package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.User
import kotlinx.coroutines.flow.SharedFlow

interface BroadheartBluetoothReceiver {
    val receivedUsers: SharedFlow<User>
    val receivedHeartRateMeasurements: SharedFlow<HeartRateMeasurement>
}
