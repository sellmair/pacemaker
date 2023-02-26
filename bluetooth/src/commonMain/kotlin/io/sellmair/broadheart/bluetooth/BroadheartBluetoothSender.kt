package io.sellmair.broadheart.bluetooth

import io.sellmair.broadheart.model.HeartRate
import io.sellmair.broadheart.model.User

interface BroadheartBluetoothSender {
    fun updateUser(user: User)
    fun updateHeartHeart(heartRate: HeartRate)
    fun updateHeartRateLimit(heartRate: HeartRate)
}
