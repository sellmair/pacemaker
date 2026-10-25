package io.sellmair.pacemaker.wear

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearGroupMemberTest {
    @Test
    fun equalityWithLimitIsCritical() {
        assertTrue(member(bpm = 145, limit = 145).isCritical)
    }

    @Test
    fun belowLimitIsNotCritical() {
        assertFalse(member(bpm = 144, limit = 145).isCritical)
    }

    @Test
    fun missingReadingOrLimitIsNotCritical() {
        assertFalse(member(bpm = null, limit = 145).isCritical)
        assertFalse(member(bpm = 160, limit = null).isCritical)
    }

    private fun member(bpm: Int?, limit: Int?) = WearGroupMember(
        userId = "user",
        name = "Runner",
        bpm = bpm,
        limit = limit,
        measuredAtEpochMillis = null,
    )
}
