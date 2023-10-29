package io.sellmair.pacemaker

import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.get
import io.sellmair.pacemaker.utils.plusAssign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class CriticalGroupState(val criticalMembers: List<UserState>) : State {
    companion object : State.Key<CriticalGroupState?> {
        override val default: CriticalGroupState? = null
    }
}

internal fun CoroutineScope.launchCriticalGroupStateActor() = launch {
    GroupState.get().collect { groupState ->
        val criticalMembers = groupState.members.filter { userState ->
            val heartRateLimit = userState.heartRateLimit ?: return@filter false
            userState.heartRate > heartRateLimit
        }

        CriticalGroupState += if (criticalMembers.isNotEmpty()) CriticalGroupState(criticalMembers) else null
    }
}
