package io.sellmair.pacemaker.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WearGroupPresentationTest {
    private val now = 1_000_000L

    @Test
    fun readingsAreMaskedAtOneMinuteBeforeCriticalOrdering() {
        val presentation = listOf(
            member("stale-critical", "Stale", bpm = 190, limit = 150, measuredAt = now - 60_001),
            member("fresh-critical", "Fresh", bpm = 150, limit = 150, measuredAt = now - 60_000),
            member("future-critical", "Future", bpm = 190, limit = 150, measuredAt = now + 1),
            member("missing-time", "Missing", bpm = 190, limit = 150, measuredAt = null),
        ).toWearGroupPresentation(now)

        assertEquals("Fresh", presentation.members.first().name)
        assertEquals(150, presentation.members.first().bpm)
        assertEquals(listOf(null, null, null), presentation.members.drop(1).map { it.bpm })
        assertEquals(
            listOf(false, false, false),
            presentation.members.drop(1).map { it.isCritical })
    }

    @Test
    fun duplicateIdentityPrefersFreshReadingAndNewestFreshDuplicate() {
        val presentation = listOf(
            member("alex", "Old Alex", bpm = 160, limit = 150, measuredAt = now - 70_000),
            member("sam", "Sam", bpm = 120, limit = 140, measuredAt = now - 500),
            member("alex", "Alex", bpm = 145, limit = 150, measuredAt = now - 2_000),
            member("alex", "Newest Alex", bpm = 151, limit = 150, measuredAt = now - 500),
            member("", "No identity", bpm = 180, limit = 150, measuredAt = now),
        ).toWearGroupPresentation(now)

        assertEquals(2, presentation.totalCount)
        assertEquals(listOf("Newest Alex", "Sam"), presentation.members.map { it.name })
        assertEquals(151, presentation.members.first().bpm)
    }

    @Test
    fun duplicateWithOnlyStaleOrFutureMeasurementsDoesNotBecomeCritical() {
        val presentation = listOf(
            member("alex", "Past Alex", bpm = 170, limit = 150, measuredAt = now - 70_000),
            member("alex", "Future Alex", bpm = 180, limit = 150, measuredAt = now + 10),
        ).toWearGroupPresentation(now)

        assertEquals(1, presentation.totalCount)
        assertNull(presentation.members.single().bpm)
        assertFalse(presentation.members.single().isCritical)
    }

    @Test
    fun sameTimestampNamedDuplicateCannotReplaceFreshMeasurementWithNoReading() {
        val fresh = member("alex", "", bpm = 160, limit = 150, measuredAt = now)
        val duplicateWithoutReading = member("alex", "Alex", bpm = null, limit = 150, measuredAt = now)

        listOf(
            listOf(fresh, duplicateWithoutReading),
            listOf(duplicateWithoutReading, fresh),
        ).forEach { duplicates ->
            val selected = duplicates.toWearGroupPresentation(now).members.single()

            assertEquals(160, selected.bpm)
            assertTrue(selected.isCritical)
        }
    }

    @Test
    fun capsRowsAtTwelveAndKeepsHonestTotal() {
        val members = (0..12).map { index ->
            member(
                userId = "user-$index",
                name = "User $index",
                bpm = 120 + index,
                limit = 130,
                measuredAt = now,
            )
        }

        val presentation = members.toWearGroupPresentation(now)

        assertEquals(13, presentation.totalCount)
        assertEquals(12, presentation.shownCount)
        assertEquals(3, presentation.members.takeWhile { it.isCritical }.size)
        assertTrue(presentation.members.drop(3).none { it.isCritical })
    }

    private fun member(
        userId: String,
        name: String,
        bpm: Int?,
        limit: Int?,
        measuredAt: Long?,
    ) = WearGroupMember(userId, name, bpm, limit, measuredAt)
}
