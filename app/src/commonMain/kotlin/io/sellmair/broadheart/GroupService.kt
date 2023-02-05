package io.sellmair.broadheart

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface GroupService {
    val state: Flow<GroupState>


    companion object {
        val instance: GroupService get() = DummyGroupService
    }
}

private val dummyGroupStatic = GroupState(
    members = listOf(
        GroupMemberState(
            user = User(
                uuid = randomUUID(),
                name = "Michael Weingartner"
            ),
            currentHeartRate = HeartRate(124),
            upperLimitHeartRate = HeartRate(140)
        ),
        GroupMemberState(
            user = User(
                uuid = randomUUID(),
                name = "Sebastian Weingartner"
            ),
            currentHeartRate = HeartRate(110),
            upperLimitHeartRate = HeartRate(143)
        ),
        GroupMemberState(
            user = User(
                uuid = randomUUID(),
                name = "Christian Andreas"
            ),
            currentHeartRate = HeartRate(135),
            upperLimitHeartRate = HeartRate(150)
        ),
    )
)

val dummyGroupState
    get() = GroupState(
        dummyGroupStatic.members + GroupMemberState(
            user = Me.user,
            currentHeartRate = HeartRate(130),
            upperLimitHeartRate = Me.myLimit
        )
    )

private object DummyGroupService : GroupService {
    override val state: Flow<GroupState>
        get() = flow {
            while (true) {
                emit(dummyGroupState)
                delay(5)
            }
        }

}
