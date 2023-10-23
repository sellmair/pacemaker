package io.sellmair.pacemaker.model

import kotlinx.datetime.Instant

data class Session(
    val id: SessionId,
    val startTime: Instant,
    val endTime: Instant?
)
