package io.sellmair.broadheart

import io.sellmair.broadheart.model.HeartRateMeasurement
import io.sellmair.broadheart.model.UserId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class DefaultGroupService(
    private val userService: UserService
) : GroupService {
    private val measurements = mutableListOf<HeartRateMeasurement>()
    private val foreignStates = mutableMapOf<UserId, GroupMember>()
    private val _groupState = MutableStateFlow(Group(emptyList()))
    override val group: StateFlow<Group> = _groupState.asStateFlow()

    override suspend fun add(measurement: HeartRateMeasurement) {
        measurements.add(measurement)
        invalidate()
    }

    override suspend fun add(foreignState: GroupMember) {
        foreignStates[foreignState.user?.id ?: return] = foreignState
    }

    override suspend fun invalidate() {
        val nextState = withContext(Dispatchers.Default) {
            calculateGroupState(userService, measurements.toList(), foreignStates.values.toList())
        }

        _groupState.emit(nextState)
    }
}