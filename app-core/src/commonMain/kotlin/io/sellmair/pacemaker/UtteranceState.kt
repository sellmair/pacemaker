package io.sellmair.pacemaker

import UtteranceEvent
import io.sellmair.pacemaker.utils.State
import io.sellmair.pacemaker.utils.get

enum class UtteranceState : State {
    Silence, Warnings, All;

    fun next(): UtteranceState {
        return UtteranceState.entries[(ordinal + 1) % UtteranceState.entries.size]
    }

    companion object Key : State.Key<UtteranceState> {
        override val default: UtteranceState = All

        suspend fun shouldBeAnnounced(type: UtteranceEvent.Type): Boolean {
            return when (type) {
                UtteranceEvent.Type.Info -> UtteranceState.get().value >= All
                UtteranceEvent.Type.Warning -> UtteranceState.get().value >= Warnings
            }
        }

        suspend fun shouldBeAnnounced(event: UtteranceEvent) = shouldBeAnnounced(event.type)
    }

}

