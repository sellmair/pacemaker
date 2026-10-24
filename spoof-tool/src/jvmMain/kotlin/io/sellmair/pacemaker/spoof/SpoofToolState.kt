package io.sellmair.pacemaker.spoof

import io.sellmair.evas.State

data class SpoofToolState(
    val status: Status = Status.Stopped,
    val message: String = "Advertiser stopped",
    val userId: String = "2412",
    val userName: String = "Felix Werner",
    val heartRate: Int = 130,
    val heartRateLimit: Int = 120,
    val recentOutput: List<String> = emptyList(),
) : State {
    enum class Status {
        Stopped,
        Starting,
        Advertising,
        Stopping,
        Error,
    }

    companion object Key : State.Key<SpoofToolState> {
        override val default = SpoofToolState()
    }
}
