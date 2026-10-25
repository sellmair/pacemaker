package io.sellmair.pacemaker

import io.sellmair.evas.State

data class WatchCompanionCandidate(
    val id: String,
    val name: String,
    val paired: Boolean = true,
    val connected: Boolean = false
)

/** Keep an authenticated phone pin stable across discovery changes. */
fun resolvePinnedWatchPhoneNode(pinnedNodeId: String?, candidates: List<WatchCompanionCandidate>): String? {
    val reachable = candidates.filter { it.connected }
    return if (pinnedNodeId != null) pinnedNodeId.takeIf { id -> reachable.any { it.id == id } }
    else reachable.singleOrNull()?.id
}

fun acceptsWatchMeasurementFrom(selectedNodeId: String?, inboundNodeId: String?): Boolean =
    selectedNodeId != null && selectedNodeId == inboundNodeId

/** A paired companion can be remembered while offline; connected means the Wear Data Layer reports it reachable. */
data class WatchCompanionDeviceState(
    val paired: Boolean = false,
    val connected: Boolean = false,
    val name: String = "Wrist watch",
    val candidates: List<WatchCompanionCandidate> = emptyList(),
    val platformAvailable: Boolean = true
) : State {
    companion object Key : State.Key<WatchCompanionDeviceState> {
        override val default = WatchCompanionDeviceState()
    }
}

const val WATCH_COMPANION_SOURCE_ID = "pacemaker-watch-companion"
