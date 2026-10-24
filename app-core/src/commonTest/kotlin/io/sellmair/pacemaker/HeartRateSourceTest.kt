package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartRateSourceTest {
    @Test fun measurementExpiresAfterFifteenSeconds() {
        val source = HeartRateSource("watch-1", HeartRateSource.Kind.ONBOARD_WATCH,
            HeartRateSource.Availability.AVAILABLE, HeartRate(140), 10_000)
        assertEquals(HeartRate(140), source.currentHeartRate(25_000))
        assertNull(source.currentHeartRate(25_001))
        assertNull(source.currentHeartRate(9_999))
    }

    @Test fun unavailableOrUntimedValuesAreNeverCurrent() {
        assertNull(HeartRateSource("sensor", HeartRateSource.Kind.BLE_SENSOR,
            HeartRateSource.Availability.DISCONNECTED, HeartRate(90), 100).currentHeartRate(100))
        assertNull(HeartRateSource("watch", HeartRateSource.Kind.ONBOARD_WATCH,
            HeartRateSource.Availability.AVAILABLE, HeartRate(90)).currentHeartRate(100))
    }

    @Test fun freshnessBoundaryFlipsToClearAfterFifteenSeconds() {
        val user = User(id = io.sellmair.pacemaker.model.UserId(1L), name = "runner")
        val state = MeState(user, HeartRate(100), HeartRate(160), measuredAtEpochMillis = 1_000)
        assertTrue(state.hasFreshHeartRate(16_000))
        assertFalse(state.hasFreshHeartRate(16_001))
        assertFalse(state.copy(heartRate = null).hasFreshHeartRate(2_000))
    }

    @Test fun sameHeartRateValueCanBeBroadcastAgainAfterExpiryClear() {
        val user = User(id = io.sellmair.pacemaker.model.UserId(1L), name = "runner")
        val first = MeState(user, HeartRate(120), HeartRate(160), measuredAtEpochMillis = 1_000)
        val recovered = first.copy(measuredAtEpochMillis = 20_000)
        assertEquals(HeartRate(120), first.heartRateForBroadcast(2_000))
        assertNull(first.heartRateForBroadcast(16_001))
        assertEquals(HeartRate(120), recovered.heartRateForBroadcast(20_001))
    }

    @Test fun selectedWatchFiltersOnlyThisPhoneOwnParticipant() {
        val watch = HeartRateSource(WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
            HeartRateSource.Availability.AVAILABLE)
        assertTrue(acceptsHeartRateMeasurement(watch, "own", "own", WATCH_COMPANION_SOURCE_ID))
        assertFalse(acceptsHeartRateMeasurement(watch, "own", "own", "strap-1"))
        assertTrue(acceptsHeartRateMeasurement(watch, "own", "other", "other-user-strap"))
    }

    @Test fun retriedCommandsAreAcceptedOnceAndCacheIsBounded() {
        val deduplicator = MessageDeduplicator(capacity = 2)
        assertTrue(deduplicator.accept("a"))
        assertFalse(deduplicator.accept("a"))
        assertTrue(deduplicator.accept("b"))
        assertTrue(deduplicator.accept("c"))
        assertTrue(deduplicator.accept("a"))
    }

    @Test fun sessionCommandsRejectRetriesAndLateStartAfterStop() {
        val gate = WatchSessionCommandGate()
        val start = WatchSessionCommand("start-1", WatchSessionCommand.Action.START, 1, "run-a", 1)
        val stop = WatchSessionCommand("stop-1", WatchSessionCommand.Action.STOP, 2, "run-a", 2)
        val lateStart = WatchSessionCommand("late-start", WatchSessionCommand.Action.START, 3, "run-a", 1)
        assertTrue(gate.accept(start))
        assertTrue(gate.accept(start)) // retry is acknowledged idempotently without reapplying Start
        assertTrue(gate.accept(stop))
        assertFalse(gate.accept(lateStart))
        val startSecondRun = WatchSessionCommand("start-2", WatchSessionCommand.Action.START, 4, "run-b", 1)
        val lateStopFirstRun = WatchSessionCommand("late-stop", WatchSessionCommand.Action.STOP, 5, "run-a", 3)
        assertTrue(gate.accept(startSecondRun))
        assertFalse(gate.accept(lateStopFirstRun))
    }

    @Test fun stopTombstoneRejectsDelayedStartWithoutStoppingAnotherRun() {
        val gate = WatchSessionCommandGate()
        val stopBeforeStart = WatchSessionCommand("stop-a", WatchSessionCommand.Action.STOP, 2, "run-a", 2)
        val delayedStart = WatchSessionCommand("start-a", WatchSessionCommand.Action.START, 1, "run-a", 1)
        assertTrue(gate.accept(stopBeforeStart))
        assertFalse(gate.accept(delayedStart))

        assertTrue(gate.accept(WatchSessionCommand("start-b", WatchSessionCommand.Action.START, 3, "run-b", 1)))
        assertFalse(gate.accept(WatchSessionCommand("late-stop-a", WatchSessionCommand.Action.STOP, 4, "run-a", 3)))
        assertFalse(gate.accept(WatchSessionCommand("duplicate-start-b", WatchSessionCommand.Action.START, 5, "run-b", 1)))
        assertTrue(gate.accept(WatchSessionCommand("stop-b", WatchSessionCommand.Action.STOP, 6, "run-b", 2)))
    }

    @Test fun duplicateAcceptedStopAcknowledgesWithoutApplyingToNextRun() {
        val gate = WatchSessionCommandGate()
        val startA = WatchSessionCommand("start-a", WatchSessionCommand.Action.START, 1, "run-a", 1)
        val stopA = WatchSessionCommand("stop-a", WatchSessionCommand.Action.STOP, 2, "run-a", 2)
        val startB = WatchSessionCommand("start-b", WatchSessionCommand.Action.START, 3, "run-b", 1)
        assertTrue(gate.decide(startA).shouldApply)
        assertTrue(gate.decide(stopA).shouldApply)
        assertTrue(gate.decide(startB).shouldApply)
        val retry = gate.decide(stopA)
        assertTrue(retry.accepted)
        assertFalse(retry.shouldApply)
        assertTrue(gate.decide(WatchSessionCommand("stop-b", WatchSessionCommand.Action.STOP, 4, "run-b", 2)).shouldApply)
    }

    @Test fun pinnedWearNodeNeverFallsBackToAnotherReachablePhone() {
        val candidates = listOf(
            WatchCompanionCandidate("phone-a", "Paired phone", connected = false),
            WatchCompanionCandidate("phone-b", "Other phone", connected = true)
        )
        assertNull(resolvePinnedWatchPhoneNode("phone-a", candidates))
        assertEquals("phone-b", resolvePinnedWatchPhoneNode(null, candidates))
        assertNull(resolvePinnedWatchPhoneNode(null, candidates + WatchCompanionCandidate("phone-c", "Third", connected = true)))
    }

    @Test fun measurementDisconnectStopCannotClearAStartedLaterRun() {
        val gate = WatchMeasurementRunGate()
        gate.start("run-a")
        assertTrue(gate.acceptsHeartRate("run-a"))
        assertFalse(gate.acceptsHeartRate("run-b"))
        assertTrue(gate.shouldStop("run-a", 1))
        gate.stopped(1)
        assertFalse(gate.acceptsHeartRate("run-a"))

        gate.start("run-b")
        assertTrue(gate.acceptsHeartRate("run-b"))
        assertFalse(gate.shouldStop("run-a", 2))
        assertTrue(gate.shouldStop("run-b", 2))
    }

    @Test fun watchSamplesRequireTheExplicitlySelectedNode() {
        assertFalse(acceptsWatchMeasurementFrom("node-a", "node-b"))
        assertTrue(acceptsWatchMeasurementFrom("node-a", "node-a"))
        assertFalse(acceptsWatchMeasurementFrom(null, "node-a"))
    }

    @Test fun measurementStopCodecCarriesRunAndGeneration() {
        val stop = WatchCompanionMessage.MeasurementStop("stop-1", 7, "run-a")
        assertEquals(stop, WatchCompanionCodec.decode(WatchCompanionCodec.encode(stop)))
        val heartRate = WatchCompanionMessage.HeartRate("hr-1", "user", 90, 100, "run-a")
        assertEquals(heartRate, WatchCompanionCodec.decode(WatchCompanionCodec.encode(heartRate)))
        assertEquals(null, WatchCompanionCodec.decode("H|old|user|90|100").let {
            (it as? WatchCompanionMessage.HeartRate)?.measurementRunId
        })
    }

    @Test fun stopTombstonesSurviveReloadAndRejectLateStart() {
        val firstProcess = WatchSessionStopTombstones()
        firstProcess.markStopped("run-a")
        val afterRestart = WatchSessionStopTombstones(firstProcess.snapshot())
        assertTrue(afterRestart.rejectsStart("run-a"))
        assertFalse(afterRestart.rejectsStart("run-b"))
        afterRestart.markStopped("run-b")
        assertEquals(setOf("run-a", "run-b"), afterRestart.snapshot())
    }

    @Test fun failedLocalStartIsNotReplayedAsSuccessfulOnRetry() {
        val gate = WatchSessionCommandGate()
        val start = WatchSessionCommand("start-failed", WatchSessionCommand.Action.START, 1, "run-a", 1)
        val first = gate.decide(start)
        assertTrue(first.accepted)
        assertTrue(first.shouldApply)
        gate.complete(start.commandId, false)
        val retry = gate.decide(start)
        assertFalse(retry.accepted)
        assertFalse(retry.shouldApply)
    }

    @Test fun failedStartRollsBackRunOwnershipButKeepsNewRunOrderValid() {
        val gate = WatchSessionCommandGate()
        val failedStart = WatchSessionCommand("start-failed-a", WatchSessionCommand.Action.START, 1, "run-a", 1)
        assertTrue(gate.decide(failedStart).shouldApply)
        gate.complete(failedStart.commandId, false)
        assertFalse(gate.decide(failedStart).accepted)
        val nextRun = WatchSessionCommand("start-b", WatchSessionCommand.Action.START, 2, "run-b", 1)
        assertTrue(gate.decide(nextRun).accepted)
        assertFalse(gate.decide(nextRun).shouldApply)
    }

    @Test fun phonePublishedRunAcceptsWatchStopOnSameLogicalRun() {
        val gate = WatchSessionCommandGate()
        val runId = phonePublishedWatchRunId("42")
        val phoneStart = WatchSessionCommand("phone-start-sql-42", WatchSessionCommand.Action.START, 1, runId, 1)
        assertTrue(gate.decide(phoneStart).shouldApply)
        gate.complete(phoneStart.commandId, true)

        val watchStop = WatchSessionCommand("watch-stop-sql-42", WatchSessionCommand.Action.STOP, 2, runId, 2)
        assertTrue(isWatchStopForRun(watchStop, requestedRunId = null, publishedRunId = runId))
        assertTrue(gate.decide(watchStop).accepted)
        assertFalse(gate.decide(watchStop).shouldApply)
        assertTrue(isWatchStopForSession(watchStop, activeSessionId = "42", requestedRunId = null,
            requestedSessionId = null, publishedRunId = runId, publishedSessionId = "42"))
        assertFalse(isWatchStopForSession(watchStop, activeSessionId = "43", requestedRunId = null,
            requestedSessionId = null, publishedRunId = runId, publishedSessionId = "42"))
    }

    @Test fun companionCodecRoundTripsEscapedNamesAndGroupTimes() {
        val message = WatchCompanionMessage.GroupSnapshot("g-1", listOf(
            WatchCompanionMessage.GroupSnapshot.Member("u1", "Zoë | runner", 182, 175, 42_000),
            WatchCompanionMessage.GroupSnapshot.Member("u2", "No rate", null, null, null)
        ))
        assertEquals(message, WatchCompanionCodec.decode(WatchCompanionCodec.encode(message)))
        val ack = WatchCompanionMessage.Ack("stop-7", true)
        assertEquals(ack, WatchCompanionCodec.decode(WatchCompanionCodec.encode(ack)))
        val disconnectedSource = WatchCompanionMessage.Configure("config-2", "u1", "Zoë", 12f, 145, sourceSelected = false)
        assertEquals(disconnectedSource, WatchCompanionCodec.decode(WatchCompanionCodec.encode(disconnectedSource)))
        assertEquals(true, (WatchCompanionCodec.decode("C|legacy|u1|Runner|12.0|145") as WatchCompanionMessage.Configure).sourceSelected)
    }

}
