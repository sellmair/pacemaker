package io.sellmair.pacemaker.spoof

import io.sellmair.evas.Event

sealed interface SpoofToolIntent : Event {
    data object Start : SpoofToolIntent
    data object Stop : SpoofToolIntent
    data class SetUserId(val value: String) : SpoofToolIntent
    data class SetUserName(val value: String) : SpoofToolIntent
    data class SetHeartRate(val value: Int) : SpoofToolIntent
    data class SetHeartRateLimit(val value: Int) : SpoofToolIntent

    data class NativeOutput(val process: Process, val line: String) : SpoofToolIntent
    data class NativeExited(val process: Process, val exitCode: Int) : SpoofToolIntent
    data object NativeStopped : SpoofToolIntent
}
