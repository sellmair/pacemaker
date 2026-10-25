package io.sellmair.pacemaker.wear

import io.sellmair.evas.State
import io.sellmair.evas.States

data class WearWorkoutState(
    val isRunning: Boolean = false,
    val isStarting: Boolean = false,
    val companionConnected: Boolean = false,
    val sourceSelected: Boolean = false,
    val heartRate: Int? = null,
    val status: String = "Ready",
    val userId: String = "",
    val userName: String = "Pacemaker",
    val colorHue: Float = 0f,
    val heartRateLimit: Int? = null,
    val group: List<WearGroupMember> = emptyList(),
    val measuredAtEpochMillis: Long? = null
) : State {
    companion object Key : State.Key<WearWorkoutState> {
        override val default = WearWorkoutState()
    }
}

data class WearGroupMember(
    val userId: String,
    val name: String,
    val bpm: Int?,
    val limit: Int?,
    val measuredAtEpochMillis: Long?
) {
    val isCritical get() = bpm != null && limit != null && bpm >= limit
}

internal var States.workout: WearWorkoutState
    get() = getState(WearWorkoutState).value
    set(value) { setState(WearWorkoutState, value) }
