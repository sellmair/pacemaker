package io.sellmair.pacemaker.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearHomePresentationTest {
    private val now = 1_000_000L

    @Test
    fun measurementIsFreshAtTheFifteenSecondBoundary() {
        val presentation = workout(
            heartRate = 142,
            measuredAtEpochMillis = now - 15_000,
        ).toWearHomePresentation(now)

        assertEquals(142, presentation.heartRate)
        assertEquals(WearReadingStatus.WITHIN_LIMIT, presentation.readingStatus)
    }

    @Test
    fun staleFutureAndUntimestampedMeasurementsAreNotShown() {
        listOf(
            workout(isRunning = true, heartRate = 142, measuredAtEpochMillis = now - 15_001),
            workout(isRunning = true, heartRate = 142, measuredAtEpochMillis = now + 1),
            workout(isRunning = true, heartRate = 142, measuredAtEpochMillis = null),
            workout(isRunning = true, heartRate = null, measuredAtEpochMillis = now),
        ).forEach { state ->
            val presentation = state.toWearHomePresentation(now)

            assertNull(presentation.heartRate)
            assertEquals(WearReadingStatus.NO_READING, presentation.readingStatus)
            assertEquals(WearHomeNotice.HEART_RATE_UNAVAILABLE, presentation.notice)
        }
    }

    @Test
    fun equalityIsAtLimitAndGreaterReadingIsAboveLimit() {
        assertEquals(
            WearReadingStatus.AT_LIMIT,
            workout(
                heartRate = 150,
                measuredAtEpochMillis = now
            ).toWearHomePresentation(now).readingStatus,
        )
        assertEquals(
            WearReadingStatus.ABOVE_LIMIT,
            workout(
                heartRate = 151,
                measuredAtEpochMillis = now
            ).toWearHomePresentation(now).readingStatus,
        )
    }

    @Test
    fun missingLimitDoesNotClaimTheReadingIsWithinLimit() {
        val presentation = workout(
            heartRate = 142,
            measuredAtEpochMillis = now,
            heartRateLimit = null,
        ).toWearHomePresentation(now)

        assertEquals(142, presentation.heartRate)
        assertEquals(WearReadingStatus.NO_LIMIT, presentation.readingStatus)
    }

    @Test
    fun configuredIdleStateRetainsReadyOrConnectedNoticeWithoutAReading() {
        val ready = workout(status = "Ready").toWearHomePresentation(now)
        val connected = workout(status = "Connected to Pacemaker").toWearHomePresentation(now)

        assertEquals(WearHomeNotice.READY, ready.notice)
        assertEquals(WearHomeNotice.CONNECTED, connected.notice)
    }

    @Test
    fun freshWristReadingAndStopRemainAvailableWhenPhoneIsDisconnected() {
        val presentation = workout(
            isRunning = true,
            companionConnected = false,
            heartRate = 142,
            measuredAtEpochMillis = now,
            status = "With phone",
        ).toWearHomePresentation(now)

        assertEquals(142, presentation.heartRate)
        assertEquals(WearReadingStatus.WITHIN_LIMIT, presentation.readingStatus)
        assertEquals(WearHomeNotice.PHONE_DISCONNECTED, presentation.connectionNotice)
        assertEquals(WearPrimaryAction.STOP, presentation.primaryAction)
    }

    @Test
    fun failedStopKeepsStopActionWhileWorkoutIsActive() {
        val presentation = workout(
            isRunning = true,
            companionConnected = false,
            status = "Could not stop workout",
        ).toWearHomePresentation(now)

        assertEquals(WearHomeNotice.STOP_FAILED, presentation.notice)
        assertEquals(WearPrimaryAction.STOP, presentation.primaryAction)
    }

    @Test
    fun importantNoticeAndConnectionStateStayIndependentOfFreshReading() {
        val state = workout(
            isRunning = true,
            companionConnected = false,
            heartRate = 142,
            measuredAtEpochMillis = now,
            status = "Session sync unavailable",
        )

        val presentation = state.toWearHomePresentation(now)

        assertEquals(142, presentation.heartRate)
        assertEquals(WearHomeNotice.SESSION_SYNC_UNAVAILABLE, presentation.notice)
        assertEquals(WearHomeNotice.PHONE_DISCONNECTED, presentation.connectionNotice)
    }

    @Test
    fun startingDisablesRepeatedStartAndUnconfiguredWorkoutOffersConnect() {
        val starting = workout(
            isStarting = true,
            status = "Starting workout",
        ).toWearHomePresentation(now)
        val unconfigured = workout(
            companionConnected = true,
            sourceSelected = false,
        ).toWearHomePresentation(now)

        assertEquals(WearPrimaryAction.STARTING, starting.primaryAction)
        assertEquals(WearHomeNotice.STARTING, starting.notice)
        assertEquals(WearPrimaryAction.CONNECT, unconfigured.primaryAction)
        assertEquals(WearHomeNotice.WATCH_NOT_SELECTED, unconfigured.notice)
    }

    private fun workout(
        isRunning: Boolean = false,
        isStarting: Boolean = false,
        companionConnected: Boolean = true,
        sourceSelected: Boolean = true,
        heartRate: Int? = null,
        measuredAtEpochMillis: Long? = null,
        heartRateLimit: Int? = 150,
        status: String = if (isRunning) "With phone" else "Ready",
    ) = WearWorkoutState(
        isRunning = isRunning,
        isStarting = isStarting,
        companionConnected = companionConnected,
        sourceSelected = sourceSelected,
        heartRate = heartRate,
        measuredAtEpochMillis = measuredAtEpochMillis,
        heartRateLimit = heartRateLimit,
        status = status,
        userId = if (sourceSelected) "runner" else "",
    )
}
