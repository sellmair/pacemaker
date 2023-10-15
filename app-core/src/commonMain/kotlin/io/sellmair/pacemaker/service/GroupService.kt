package io.sellmair.pacemaker.service

import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.GroupMember
import io.sellmair.pacemaker.model.HeartRateMeasurement
import kotlinx.coroutines.flow.StateFlow

interface GroupService {
    val group: StateFlow<Group>
    suspend fun add(measurement: HeartRateMeasurement)
    suspend fun add(foreignState: GroupMember)
    suspend fun invalidate()
}
