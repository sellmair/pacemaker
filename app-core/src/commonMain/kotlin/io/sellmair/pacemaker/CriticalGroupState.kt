package io.sellmair.pacemaker

import io.sellmair.evas.State
import io.sellmair.evas.collect
import io.sellmair.evas.launchState
import kotlinx.coroutines.CoroutineScope

data class CriticalGroupState(val criticalMembers: List<UserState>) : State {
    companion object : State.Key<CriticalGroupState?> {
        override val default: CriticalGroupState? = null
    }
}

internal fun CoroutineScope.launchCriticalGroupStateActor() = launchState(CriticalGroupState) {
    GroupState.collect { groupState ->
        val criticalMembers = groupState.members.filter { userState ->
            val heartRateLimit = userState.heartRateLimit ?: return@filter false
            userState.heartRate > heartRateLimit
        }

        (if (criticalMembers.isNotEmpty()) CriticalGroupState(criticalMembers) else null).emit()
    }
}
