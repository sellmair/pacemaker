package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.Hue
import io.sellmair.pacemaker.model.User

interface PacemakerBluetoothWritable {
    suspend fun setUser(user: User)
    suspend fun setHeartRate(heartRate: HeartRate)
    /** Clears the readable/advertised measurement while keeping the characteristic UUID unchanged. */
    suspend fun clearHeartRate()
    suspend fun setHeartRateLimit(heartRate: HeartRate)
    suspend fun setColorHue(hue: Hue)
}
