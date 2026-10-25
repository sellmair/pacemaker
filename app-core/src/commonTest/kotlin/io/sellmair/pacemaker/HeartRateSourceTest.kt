package io.sellmair.pacemaker

import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HeartRateSourceTest {
    @Test fun measurementExpiresAfterOneMinute() {
        val source = HeartRateSource("watch-1", HeartRateSource.Kind.ONBOARD_WATCH,
            HeartRateSource.Availability.AVAILABLE, HeartRate(140), 10_000)
        assertEquals(HeartRate(140), source.currentHeartRate(70_000))
        assertNull(source.currentHeartRate(70_001))
        assertNull(source.currentHeartRate(9_999))
    }

    @Test fun unavailableOrUntimedValuesAreNeverCurrent() {
        assertNull(HeartRateSource("sensor", HeartRateSource.Kind.BLE_SENSOR,
            HeartRateSource.Availability.DISCONNECTED, HeartRate(90), 100).currentHeartRate(100))
        assertNull(HeartRateSource("watch", HeartRateSource.Kind.ONBOARD_WATCH,
            HeartRateSource.Availability.AVAILABLE, HeartRate(90)).currentHeartRate(100))
    }

    @Test fun freshnessBoundaryFlipsToClearAfterOneMinute() {
        val user = User(id = io.sellmair.pacemaker.model.UserId(1L), name = "runner")
        val state = MeState(user, HeartRate(100), HeartRate(160), measuredAtEpochMillis = 1_000)
        assertTrue(state.hasFreshHeartRate(61_000))
        assertFalse(state.hasFreshHeartRate(61_001))
        assertFalse(state.copy(heartRate = null).hasFreshHeartRate(2_000))
    }

    @Test fun sameHeartRateValueCanBeBroadcastAgainAfterExpiryClear() {
        val user = User(id = io.sellmair.pacemaker.model.UserId(1L), name = "runner")
        val first = MeState(user, HeartRate(120), HeartRate(160), measuredAtEpochMillis = 1_000)
        val recovered = first.copy(measuredAtEpochMillis = 80_000)
        assertEquals(HeartRate(120), first.heartRateForBroadcast(2_000))
        assertNull(first.heartRateForBroadcast(61_001))
        assertEquals(HeartRate(120), recovered.heartRateForBroadcast(80_001))
    }

    @Test fun selectedWatchFiltersOnlyThisPhoneOwnParticipant() {
        val watch = HeartRateSource(WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
            HeartRateSource.Availability.AVAILABLE)
        assertTrue(acceptsHeartRateMeasurement(watch, "own", "own", WATCH_COMPANION_SOURCE_ID))
        assertFalse(acceptsHeartRateMeasurement(watch, "own", "own", "strap-1"))
        assertTrue(acceptsHeartRateMeasurement(watch, "own", "other", "other-user-strap"))
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
        assertTrue(shouldStopWatchMeasurement("run-a", 0, "run-a", 1))
        assertFalse(shouldStopWatchMeasurement("run-b", 1, "run-a", 2))
        assertTrue(shouldStopWatchMeasurement("run-b", 1, "run-b", 2))
        assertFalse(shouldStopWatchMeasurement("run-b", 2, "run-b", 2))
        assertTrue(shouldStopWatchMeasurement("run-b", 2, null, 3))
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
        val noRun = WatchCompanionMessage.HeartRate("hr-2", "user", null, 101, null)
        assertEquals(noRun, WatchCompanionCodec.decode(WatchCompanionCodec.encode(noRun)))
        val untargetedStop = WatchCompanionMessage.MeasurementStop("stop-2", 8, null)
        assertEquals(untargetedStop, WatchCompanionCodec.decode(WatchCompanionCodec.encode(untargetedStop)))
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
    }

    @Test fun companionCodecRejectsIncompleteOrMalformedFrames() {
        assertNull(WatchCompanionCodec.decode("P"))
        assertNull(WatchCompanionCodec.decode("C|config|u1|Runner|12.0|145"))
        assertNull(WatchCompanionCodec.decode("H|sample|u1|90|100"))
        assertNull(WatchCompanionCodec.decode("M|stop|8"))
        assertNull(WatchCompanionCodec.decode("A|stop|true"))
        assertNull(WatchCompanionCodec.decode("C|config|u1|Runner|12.0|145|unknown"))
        assertNull(WatchCompanionCodec.decode("H|sample|u1|90|100|run|extra"))
        assertNull(WatchCompanionCodec.decode("H|sample|u1|bad|100|run"))
        assertNull(WatchCompanionCodec.decode("G|group|1|u1|Runner|bad|150|100"))
        assertNull(WatchCompanionCodec.decode("G|group|1|u1|Runner|90|bad|100"))
        assertNull(WatchCompanionCodec.decode("G|group|1|u1|Runner|90|150|bad"))
        assertNull(WatchCompanionCodec.decode("G|group|858993460|u1|Runner||"))
    }

}
