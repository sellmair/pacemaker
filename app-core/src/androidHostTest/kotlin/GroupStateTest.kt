import io.sellmair.evas.*
import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackage
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.invoke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import utils.createInMemoryDatabase
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GroupStateTest {
    private val userService = SqlUserService(createInMemoryDatabase(), UserId(1))
    private val other = User(UserId(2), "Other runner")

    @Test
    fun `sensor and group readings survive a minute without updates then show lost peers for another minute`() = test { clock ->
        val me = userService.me()
        val sensorId = HeartRateSensorId("me")
        userService.linkSensor(me, sensorId)
        userService.saveUser(other)
        HeartRateMeasurementEvent(HeartRate(128), sensorId, clock.now()).emit()
        broadcast(clock)
        testScheduler.runCurrent()
        assertEquals(2, GroupState.value().members.size)

        testScheduler.advanceTimeBy(59_999)
        testScheduler.runCurrent()
        assertEquals(HeartRate(128), MeState.value()?.heartRate)
        assertFalse(GroupState.value().members.single { it.user.id == other.id }.isConnectionLost)

        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertNull(MeState.value()?.heartRate)
        val lost = GroupState.value().members.single()
        assertEquals(other.id, lost.user.id)
        assertTrue(lost.isConnectionLost)

        testScheduler.advanceTimeBy(59_999)
        testScheduler.runCurrent()
        assertEquals(lost, GroupState.value().members.single())
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.isEmpty())
    }

    @Test
    fun `sparse unchanged broadcasts refresh the full grace period`() = test { clock ->
        userService.saveUser(other)
        repeat(3) {
            broadcast(clock)
            testScheduler.runCurrent()
            testScheduler.advanceTimeBy(55_000)
            testScheduler.runCurrent()
            assertFalse(GroupState.value().members.single().isConnectionLost)
        }
        testScheduler.advanceTimeBy(5_000)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.single().isConnectionLost)
    }

    @Test
    fun `recovery clears lost status and cancels the old removal`() = test { clock ->
        userService.saveUser(other)
        broadcast(clock)
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(90_000)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.single().isConnectionLost)
        broadcast(clock)
        testScheduler.runCurrent()
        assertFalse(GroupState.value().members.single().isConnectionLost)
        testScheduler.advanceTimeBy(30_000)
        testScheduler.runCurrent()
        assertFalse(GroupState.value().members.single().isConnectionLost)
        testScheduler.advanceTimeBy(30_000)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.single().isConnectionLost)
        testScheduler.advanceTimeBy(60_000)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.isEmpty())
    }

    @Test
    fun `other users direct sensors get the same grace and lost retention as broadcasts`() = test { clock ->
        userService.saveUser(other)
        val sensorId = HeartRateSensorId("other-sensor")
        userService.linkSensor(other, sensorId)
        HeartRateMeasurementEvent(HeartRate(142), sensorId, clock.now()).emit()
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(59_999)
        testScheduler.runCurrent()
        assertFalse(GroupState.value().members.single().isConnectionLost)
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.single().isConnectionLost)
        testScheduler.advanceTimeBy(60_000)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.isEmpty())
    }

    @Test
    fun `transport drop keeps own measurement but selecting another source clears it`() = test { clock ->
        val me = userService.me()
        val sensorId = HeartRateSensorId(WATCH_COMPANION_SOURCE_ID)
        userService.linkSensor(me, sensorId)
        val source = HeartRateSource(WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
            HeartRateSource.Availability.AVAILABLE)
        currentCoroutineContext().statesOrThrow.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(source))
        testScheduler.runCurrent()
        HeartRateMeasurementEvent(HeartRate(120), sensorId, clock.now()).emit()
        testScheduler.runCurrent()
        currentCoroutineContext().statesOrThrow.setState(SelectedHeartRateSourceState,
            SelectedHeartRateSourceState(source.copy(availability = HeartRateSource.Availability.DISCONNECTED)))
        testScheduler.runCurrent()
        assertEquals(HeartRate(120), MeState.value()?.heartRate)
        assertEquals(HeartRate(120), GroupState.value().members.single().heartRate)
        currentCoroutineContext().statesOrThrow.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(
            HeartRateSource("another-sensor", HeartRateSource.Kind.BLE_SENSOR, HeartRateSource.Availability.CONNECTING)))
        testScheduler.runCurrent()
        assertNull(MeState.value()?.heartRate)
        assertTrue(GroupState.value().members.isEmpty())
    }

    @Test
    fun `retained lost peers do not produce heart rate warnings`() = test { clock ->
        launchCriticalGroupStateActor()
        launchHeartRateUtteranceProducer()
        val warnings = mutableListOf<HeartRateUtteranceRequest.SlowDownHeartRateUtterance>()
        collectEventsAsync<HeartRateUtteranceRequest.SlowDownHeartRateUtterance> { warnings += it }
        testScheduler.runCurrent()
        val member = UserState(other, false, HeartRate(190), HeartRate(150), UserColors.default(other.id),
            clock.now().toEpochMilliseconds(), isConnectionLost = true)
        currentCoroutineContext().statesOrThrow.setState(GroupState, GroupState(listOf(member)))
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(15_000)
        testScheduler.runCurrent()
        assertTrue(warnings.isEmpty())
        assertNull(CriticalGroupState.value())
        currentCoroutineContext().statesOrThrow.setState(GroupState, GroupState(listOf(member.copy(isConnectionLost = false))))
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(15_000)
        testScheduler.runCurrent()
        assertEquals(1, warnings.size)
        assertEquals(other.id, CriticalGroupState.value()?.criticalMembers?.single()?.user?.id)
    }

    @Test
    fun `selected watch rejects own strap but keeps other users sensors`() = test { clock ->
        val me = userService.me()
        val ownStrap = HeartRateSensorId("own-strap")
        val otherStrap = HeartRateSensorId("other-strap")
        userService.linkSensor(me, ownStrap)
        userService.saveUser(other)
        userService.linkSensor(other, otherStrap)
        currentCoroutineContext().statesOrThrow.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(
            HeartRateSource(WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH, HeartRateSource.Availability.CONNECTING)))
        testScheduler.runCurrent()
        HeartRateMeasurementEvent(HeartRate(101), ownStrap, clock.now()).emit()
        HeartRateMeasurementEvent(HeartRate(142), otherStrap, clock.now()).emit()
        testScheduler.runCurrent()
        assertEquals(other.id, GroupState.value().members.single().user.id)
    }

    private suspend fun broadcast(clock: Clock) {
        PacemakerBroadcastPackageEvent(PacemakerBroadcastPackage(
            receivedTime = clock.now(), deviceId = BleDeviceId("peer"), userId = other.id,
            userName = other.name, heartRate = HeartRate(150), heartRateLimit = HeartRate(175), userColorHue = null
        )).emit()
    }

    private fun test(block: suspend TestScope.(Clock) -> Unit) = runTest(
        Events() + States() + DatabaseBackgroundDispatcher(Dispatchers.Unconfined)
    ) {
        val clock = SchedulerClock(testScheduler)
        launchMeStateActor(userService, clock)
        launchGroupStateActor(userService, currentCoroutineContext(), clock)
        testScheduler.runCurrent()
        try {
            block(clock)
        } finally {
            currentCoroutineContext().cancelChildren()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(1_800_000_000_000L + scheduler.currentTime)
}
