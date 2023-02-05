package io.sellmair.broadheart

data class GroupState(val members: List<GroupMemberState>)

class GroupMemberState(
    val user: User,
    val currentHeartRate: HeartRate?,
    val upperLimitHeartRate: HeartRate
)