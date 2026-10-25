package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class WatchCompanionSnapshotsTest {
    @Test fun configurationIdentityChangesWithColorButNotLiveHeartRate() {
        val me = MeState(User(UserId(1), "Runner"), HeartRate(120), HeartRate(160))
        val first = watchConfigure(me, MeColorState(UserColors.fromHue(40f)), true)
        val changedRate = watchConfigure(me.copy(heartRate = HeartRate(130)), MeColorState(UserColors.fromHue(40f)), true)
        val changedColor = watchConfigure(me, MeColorState(UserColors.fromHue(70f)), true)
        assertEquals(first, changedRate)
        assertNotEquals(first.messageId, changedColor.messageId)
        assertNotEquals(first, watchConfigure(me, MeColorState(UserColors.fromHue(40f)), false))
    }

    @Test fun groupSnapshotClearsLostAndStaleRates() {
        val now = 100_000L
        fun member(id: Long, measuredAt: Long, lost: Boolean = false) = UserState(
            User(UserId(id), "Runner $id"), false, HeartRate(180), HeartRate(170),
            UserColors.fromHue(20f), measuredAt, lost
        )
        val snapshot = watchGroupSnapshot(GroupState(listOf(
            member(1, now - 1_000), member(2, now - 61_000), member(3, now - 1_000, lost = true)
        )), now)
        assertEquals(180, snapshot.members.first { it.userId == "1" }.bpm)
        assertNull(snapshot.members.first { it.userId == "2" }.bpm)
        assertNull(snapshot.members.first { it.userId == "3" }.bpm)
    }
}
