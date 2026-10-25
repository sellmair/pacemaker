package io.sellmair.pacemaker

/** The same mapping serves initial Hello replies and subsequent state updates. */
internal fun watchConfigure(me: MeState, color: MeColorState?, sourceSelected: Boolean): WatchCompanionMessage.Configure {
    val hue = color?.color?.hue ?: UserColors.defaultHue(me.me.id)
    return WatchCompanionMessage.Configure(
        messageId = "config-${me.me.id.value}-${me.me.name.hashCode()}-${me.heartRateLimit.value.toInt()}-$hue-$sourceSelected",
        userId = me.me.id.value.toString(),
        userName = me.me.name,
        colorHue = hue,
        heartRateLimit = me.heartRateLimit.value.toInt(),
        sourceSelected = sourceSelected
    )
}

internal fun watchGroupSnapshot(group: GroupState, nowEpochMillis: Long): WatchCompanionMessage.GroupSnapshot {
    val members = group.members.filterNot { it.isMe }
        .sortedByDescending { it.heartRateLimit != null && it.heartRate >= it.heartRateLimit }
        .take(5)
        .map { member ->
            val fresh = nowEpochMillis - member.measuredAtEpochMillis in
                0..HeartRateSource.MAX_AGE.inWholeMilliseconds
            WatchCompanionMessage.GroupSnapshot.Member(
                userId = member.user.id.value.toString(),
                name = member.user.name,
                bpm = member.heartRate.value.toInt().takeIf { fresh && !member.isConnectionLost },
                limit = member.heartRateLimit?.value?.toInt(),
                measuredAtEpochMillis = member.measuredAtEpochMillis
            )
        }
    return WatchCompanionMessage.GroupSnapshot("group-$nowEpochMillis", members)
}
