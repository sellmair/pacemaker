package io.sellmair.pacemaker.model

import kotlin.time.Instant

data class Session(
    val id: SessionId,
    val startTime: Instant,
    val endTime: Instant?
)
