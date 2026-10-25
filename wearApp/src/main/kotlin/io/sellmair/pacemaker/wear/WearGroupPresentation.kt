package io.sellmair.pacemaker.wear

import io.sellmair.pacemaker.HeartRateSource

internal const val WEAR_GROUP_DISPLAY_LIMIT = 12

internal data class WearGroupPresentation(
    val members: List<WearGroupMember>,
    val totalCount: Int,
) {
    val shownCount: Int get() = members.size
}

internal fun List<WearGroupMember>.toWearGroupPresentation(
    nowEpochMillis: Long,
): WearGroupPresentation {
    val maxAgeMillis = HeartRateSource.MAX_AGE.inWholeMilliseconds
    val earliestFreshTimestamp = nowEpochMillis - maxAgeMillis

    data class Candidate(
        val member: WearGroupMember,
        val freshBpm: Int?,
        val usableTimestamp: Long,
        val originalIndex: Int,
    )

    val bestByIdentity = linkedMapOf<String, Candidate>()
    forEachIndexed { index, member ->
        if (member.userId.isBlank()) return@forEachIndexed

        val timestamp = member.measuredAtEpochMillis
        val usableTimestamp = timestamp?.takeIf { it <= nowEpochMillis } ?: Long.MIN_VALUE
        val freshBpm = member.bpm.takeIf {
            timestamp != null &&
                    timestamp in earliestFreshTimestamp..nowEpochMillis
        }
        val candidate = Candidate(member, freshBpm, usableTimestamp, index)
        val current = bestByIdentity[member.userId]
        val isBetter = current == null || when {
            candidate.freshBpm != null && current.freshBpm == null -> true
            candidate.freshBpm == null && current.freshBpm != null -> false
            candidate.usableTimestamp != current.usableTimestamp ->
                candidate.usableTimestamp > current.usableTimestamp

            else -> candidate.member.name.isNotBlank() && current.member.name.isBlank()
        }
        if (isBetter) bestByIdentity[member.userId] = candidate
    }

    val uniqueMembers = bestByIdentity.values
        .sortedBy { it.originalIndex }
        .map { it.member.copy(bpm = it.freshBpm) }
    val orderedMembers = uniqueMembers.sortedByDescending { it.isCritical }

    return WearGroupPresentation(
        members = orderedMembers.take(WEAR_GROUP_DISPLAY_LIMIT),
        totalCount = orderedMembers.size,
    )
}
