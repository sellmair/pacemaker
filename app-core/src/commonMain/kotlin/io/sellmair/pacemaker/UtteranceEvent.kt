package io.sellmair.pacemaker

import io.sellmair.evas.Event

data class UtteranceEvent(
    val type: Type,
    val message: String
): Event {
    enum class Type {
        Info, Warning
    }
}
