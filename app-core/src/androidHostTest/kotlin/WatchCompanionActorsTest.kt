import android.content.SharedPreferences
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.emit
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.UserId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import utils.createInMemoryDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class WatchCompanionActorsTest {
    @Test fun acknowledgementsMatchBothPeerAndMessageId() {
        val acknowledgements = WatchAcknowledgements()
        val waiter = acknowledgements.register("watch-a", "stop-1")!!
        assertNull(acknowledgements.register("watch-b", "stop-1"))
        acknowledgements.acknowledge("watch-b", WatchCompanionMessage.Ack("stop-1", true))
        acknowledgements.acknowledge("watch-a", WatchCompanionMessage.Ack("other", true))
        assertFalse(waiter.isCompleted)
        acknowledgements.acknowledge("watch-a", WatchCompanionMessage.Ack("stop-1", true))
        assertTrue(waiter.isCompleted)
    }

    @Test fun inboundReadingsRequireSelectedPeerUserAndFreshTime() = runTest(Events() + States()) {
        val prefs = MemoryPreferences()
        val transport = FakeWatchTransport()
        val users = SqlUserService(createInMemoryDatabase(), UserId(1))
        val actorScope = CoroutineScope(backgroundScope.coroutineContext + coroutineContext.eventsOrThrow + coroutineContext.statesOrThrow)
        val delivery = WatchMeasurementStopDelivery(MemoryStopStore(prefs), actorScope, transport)
        WatchCompanionMessageActor(prefs, actorScope, transport, delivery, users).start()
        val states = coroutineContext.statesOrThrow
        states.setState(SelectedHeartRateSourceState, selectedWatch())
        val now = Clock.System.now().toEpochMilliseconds()
        val ownId = users.me().id.value.toString()

        WatchCompanionInboundMessage("other", reading(ownId, 120, now, "run-a")).emit()
        WatchCompanionInboundMessage("watch-a", reading("wrong-user", 120, now, "run-a")).emit()
        WatchCompanionInboundMessage("watch-a", reading(ownId, 120, now - 61_000, "run-a")).emit()
        assertNull(states.getState(SelectedHeartRateSourceState).value.source?.heartRate)

        WatchCompanionInboundMessage("watch-a", reading(ownId, 123, now, "run-a")).emit()
        assertEquals(123, states.getState(SelectedHeartRateSourceState).value.source?.heartRate?.value?.toInt())
        assertEquals("run-a", prefs.watchActiveRunId)
    }

    @Test fun clearFromOldRunDoesNotClearNewRun() = runTest(Events() + States()) {
        val prefs = MemoryPreferences()
        val transport = FakeWatchTransport()
        val users = SqlUserService(createInMemoryDatabase(), UserId(1))
        val actorScope = CoroutineScope(backgroundScope.coroutineContext + coroutineContext.eventsOrThrow + coroutineContext.statesOrThrow)
        val delivery = WatchMeasurementStopDelivery(MemoryStopStore(prefs), actorScope, transport)
        WatchCompanionMessageActor(prefs, actorScope, transport, delivery, users).start()
        val states = coroutineContext.statesOrThrow
        states.setState(SelectedHeartRateSourceState, selectedWatch())
        val now = Clock.System.now().toEpochMilliseconds()
        val ownId = users.me().id.value.toString()

        WatchCompanionInboundMessage("watch-a", reading(ownId, 121, now, "run-old")).emit()
        WatchCompanionInboundMessage("watch-a", reading(ownId, 144, now, "run-new")).emit()
        WatchCompanionInboundMessage("watch-a", reading(ownId, null, now, "run-old")).emit()

        assertEquals(144, states.getState(SelectedHeartRateSourceState).value.source?.heartRate?.value?.toInt())
        assertEquals("run-new", prefs.watchActiveRunId)
        assertTrue("run-old" in prefs.watchStoppedRunIds)
    }

    @Test fun switchingSourceWhileSensorLinkSuspendsDoesNotRestoreWatchReading() = runTest(Events() + States()) {
        val prefs = MemoryPreferences()
        val transport = FakeWatchTransport()
        val delegate = SqlUserService(createInMemoryDatabase(), UserId(1))
        val linkStarted = CompletableDeferred<Unit>()
        val resumeLink = CompletableDeferred<Unit>()
        val users = object : UserService by delegate {
            override suspend fun linkSensor(user: io.sellmair.pacemaker.model.User,
                                            sensorId: io.sellmair.pacemaker.model.HeartRateSensorId) {
                linkStarted.complete(Unit)
                resumeLink.await()
                delegate.linkSensor(user, sensorId)
            }
        }
        val actorScope = CoroutineScope(backgroundScope.coroutineContext + coroutineContext.eventsOrThrow + coroutineContext.statesOrThrow)
        val delivery = WatchMeasurementStopDelivery(MemoryStopStore(prefs), actorScope, transport)
        WatchCompanionMessageActor(prefs, actorScope, transport, delivery, users).start()
        val readings = mutableListOf<HeartRateMeasurementEvent>()
        actorScope.collectEventsAsync<HeartRateMeasurementEvent> { readings += it }
        val states = coroutineContext.statesOrThrow
        states.setState(SelectedHeartRateSourceState, selectedWatch())

        val sending = async {
            WatchCompanionInboundMessage("watch-a", reading(
                delegate.me().id.value.toString(), 123, Clock.System.now().toEpochMilliseconds(), "run-a"
            )).emit()
        }
        linkStarted.await()
        states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(null))
        resumeLink.complete(Unit)
        sending.await()

        assertNull(states.getState(SelectedHeartRateSourceState).value.source)
        assertTrue(readings.isEmpty())
    }

    @Test fun discoveryUsesCurrentSelectionAfterStopDrainSuspends() = runTest(Events() + States()) {
        val prefs = MemoryPreferences()
        prefs.lastSelectedWatchNodeId = "watch-a"
        val transport = FakeWatchTransport()
        val actorScope = CoroutineScope(backgroundScope.coroutineContext + coroutineContext.eventsOrThrow + coroutineContext.statesOrThrow)
        val delivery = WatchMeasurementStopDelivery(MemoryStopStore(prefs), actorScope, transport)
        delivery.enqueue("watch-a")
        val publisher = WatchCompanionStatePublisher(prefs, actorScope, transport, delivery)
        val states = coroutineContext.statesOrThrow
        states.setState(SelectedHeartRateSourceState, selectedWatch())
        val discovery = WatchCompanionDiscoveryState(listOf(
            WatchCompanionCandidate("watch-a", "A", paired = true, connected = true),
            WatchCompanionCandidate("watch-b", "B", paired = true, connected = false)
        ))
        states.setState(WatchCompanionDiscoveryState, discovery)

        val publishing = async { publisher.onDiscovery(discovery) }
        runCurrent()
        assertEquals("watch-a", transport.sent.single().first)
        states.setState(SelectedHeartRateSourceState, selectedWatch("watch-b"))
        states.setState(WatchCompanionDiscoveryState, discovery.copy(candidates = listOf(
            WatchCompanionCandidate("watch-b", "B updated", paired = true, connected = false)
        )))
        val stop = transport.sent.single().second as WatchCompanionMessage.MeasurementStop
        transport.onMessage("watch-a", WatchCompanionMessage.Ack(stop.messageId, true))
        publishing.await()

        val selected = states.getState(SelectedHeartRateSourceState).value.source
        assertEquals("watch-b", selected?.companionDeviceId)
        assertEquals(HeartRateSource.Availability.DISCONNECTED, selected?.availability)
        val device = states.getState(WatchCompanionDeviceState).value
        assertEquals("B updated", device.name)
        assertFalse(device.connected)
    }

    @Test fun queuedStopSurvivesReloadAndRequiresMatchingPeerAck() = runTest(Events() + States()) {
        val prefs = MemoryPreferences()
        prefs.activateWatchRun("run-a", "watch-a")
        val transport = FakeWatchTransport()
        val actorScope = CoroutineScope(backgroundScope.coroutineContext + coroutineContext.eventsOrThrow + coroutineContext.statesOrThrow)
        val store = MemoryStopStore(prefs)
        val first = WatchMeasurementStopDelivery(store, actorScope, transport)
        first.enqueue("watch-a")
        val reloaded = WatchMeasurementStopDelivery(store, actorScope, transport)
        assertEquals(setOf("watch-a"), reloaded.pendingTargets())
        val users = SqlUserService(createInMemoryDatabase(), UserId(1))
        WatchCompanionMessageActor(prefs, actorScope, transport, reloaded, users).start()

        val draining = async { reloaded.drain() }
        runCurrent()
        val command = transport.sent.single().second as WatchCompanionMessage.MeasurementStop
        assertEquals("run-a", command.measurementRunId)
        WatchCompanionInboundMessage("wrong-peer", WatchCompanionMessage.Ack(
            command.messageId, true, "run-a"
        )).emit()
        assertFalse(draining.isCompleted)
        assertEquals(setOf("watch-a"), reloaded.pendingTargets())

        WatchCompanionInboundMessage("watch-a", WatchCompanionMessage.Ack(
            command.messageId, true, "run-a"
        )).emit()
        assertTrue(draining.await())
        assertTrue(reloaded.pendingTargets().isEmpty())
        assertNull(prefs.watchActiveRunId)
        assertTrue("run-a" in prefs.watchStoppedRunIds)
    }

    private fun selectedWatch(nodeId: String = "watch-a") = SelectedHeartRateSourceState(HeartRateSource(
        WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
        HeartRateSource.Availability.AVAILABLE, companionDeviceId = nodeId
    ))

    private fun reading(userId: String, bpm: Int?, time: Long, runId: String) =
        WatchCompanionMessage.HeartRate("reading-$runId-$bpm", userId, bpm, time, runId)
}

private class FakeWatchTransport : WatchCompanionTransport {
    val sent = mutableListOf<Pair<String, WatchCompanionMessage>>()
    private var pending: Triple<String, String, CompletableDeferred<Boolean>>? = null

    override suspend fun sendTo(nodeId: String, message: WatchCompanionMessage): Boolean {
        sent += nodeId to message
        return true
    }

    override suspend fun sendReliableToOutcome(
        nodeId: String, message: WatchCompanionMessage, retries: Int
    ): Boolean? {
        sent += nodeId to message
        val result = CompletableDeferred<Boolean>()
        pending = Triple(nodeId, message.messageId, result)
        return result.await()
    }

    override fun onMessage(nodeId: String, message: WatchCompanionMessage) {
        val ack = message as? WatchCompanionMessage.Ack ?: return
        pending?.takeIf { it.first == nodeId && it.second == ack.messageId }
            ?.third?.complete(ack.accepted)
    }
}

private class MemoryStopStore(private val prefs: SharedPreferences) : WatchMeasurementStopStore {
    private var queue = emptyList<PendingMeasurementStop>()
    private var generation = 0L
    override fun load(): List<PendingMeasurementStop> = queue.toList()
    override fun save(queue: List<PendingMeasurementStop>) { this.queue = queue.toList() }
    override fun nextGeneration(): Long = ++generation
    override fun activeRunFor(nodeId: String): String? = prefs.watchActiveRunId
        .takeIf { prefs.watchActiveRunNodeId == nodeId }
    override fun markStopped(nodeId: String, runId: String?) {
        if (runId == null) return
        prefs.markWatchRunStopped(
            runId, clearActive = prefs.watchActiveRunId == runId && prefs.watchActiveRunNodeId == nodeId
        )
    }
}

private class MemoryPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        values[key] as? Set<String> ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = Editor()

    private inner class Editor : SharedPreferences.Editor {
        private val updates = mutableMapOf<String, Any?>()
        private var clear = false
        override fun putString(key: String?, value: String?) = apply { updates[key.orEmpty()] = value }
        override fun putStringSet(key: String?, values: Set<String>?) = apply { updates[key.orEmpty()] = values?.toSet() }
        override fun putInt(key: String?, value: Int) = apply { updates[key.orEmpty()] = value }
        override fun putLong(key: String?, value: Long) = apply { updates[key.orEmpty()] = value }
        override fun putFloat(key: String?, value: Float) = apply { updates[key.orEmpty()] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { updates[key.orEmpty()] = value }
        override fun remove(key: String?) = apply { updates[key.orEmpty()] = null }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clear) values.clear()
            updates.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
        }
    }
}
