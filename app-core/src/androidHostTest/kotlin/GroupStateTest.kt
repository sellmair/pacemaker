import io.sellmair.evas.*
import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackage
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.value
import io.sellmair.pacemaker.utils.invoke
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import utils.createInMemoryDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GroupStateTest {

    private val userService = SqlUserService(createInMemoryDatabase(), UserId(1))

    @Test
    fun `sample 0`() = test {
        val meSensorId = HeartRateSensorId("me")
        val me = userService.me()
        userService.linkSensor(me, meSensorId)
        val measuredAt = Clock.System.now()
        HeartRateMeasurementEvent(HeartRate(128f), meSensorId, measuredAt).emit()

        launch {
            GroupState.collect { println("(${testScheduler.currentTime}ms) s: $it") }
        }

        val state = GroupState.flow().first { it.members.isNotEmpty() }
        println("(${testScheduler.currentTime}ms) Received first state with non-empty members")

        assertEquals(
            GroupState(
                listOf(
                    UserState(
                        user = me,
                        isMe = true,
                        heartRate = HeartRate(128f),
                        heartRateLimit = UserService.NewUserHeartRateLimit.value(),
                        color = UserColors.default(me.id),
                        measuredAtEpochMillis = measuredAt.toEpochMilliseconds()
                    )
                )
            ), state,
            "Expect first state with non-empty members to reflect the previously emitted '${HeartRateMeasurementEvent::class}'"
        )

        println("(${testScheduler.currentTime}ms) Testing current state value...")
        assertEquals(
            state, GroupState.value(),
            "(${testScheduler.currentTime}ms) Expect the current value of 'GroupState' to be the recently emitted state"
        )

        println("(${testScheduler.currentTime}ms) Waiting until the Group state resets..")
        GroupState.flow().first { it == GroupState.default }

        println("(${testScheduler.currentTime}ms) Group state was reset!")
        assertEquals(GroupState.default, GroupState.value())
    }

    @Test
    fun `selected watch rejects own strap but keeps other users sensors`() = test {
        val me = userService.me()
        val ownStrap = HeartRateSensorId("own-strap")
        userService.linkSensor(me, ownStrap)
        val other = io.sellmair.pacemaker.model.User(UserId(2), "Other runner")
        userService.saveUser(other)
        val otherStrap = HeartRateSensorId("other-strap")
        userService.linkSensor(other, otherStrap)
        currentCoroutineContext().statesOrThrow.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(HeartRateSource(
            WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
            HeartRateSource.Availability.CONNECTING
        )))
        MeState.flow().first { it != null }
        val time = Clock.System.now()
        HeartRateMeasurementEvent(HeartRate(101), ownStrap, time).emit()
        HeartRateMeasurementEvent(HeartRate(142), otherStrap, time).emit()

        val state = GroupState.flow().first { it.members.any { member -> member.user.id == other.id } }
        assertFalse(state.members.any { it.user.id == me.id })
        assertEquals(HeartRate(142), state.members.single { it.user.id == other.id }.heartRate)
    }

    @Test
    fun `own sensor expires in fifteen seconds while remote participant remains for one minute`() = test {
        val me = userService.me()
        val ownSensor = HeartRateSensorId("own-sensor")
        userService.linkSensor(me, ownSensor)
        val other = io.sellmair.pacemaker.model.User(UserId(3), "Remote runner")
        userService.saveUser(other)
        val time = Clock.System.now()
        HeartRateMeasurementEvent(HeartRate(120), ownSensor, time).emit()
        PacemakerBroadcastPackageEvent(PacemakerBroadcastPackage(
            receivedTime = time, deviceId = BleDeviceId("peer"), userId = other.id,
            userName = other.name, heartRate = HeartRate(150), heartRateLimit = HeartRate(175), userColorHue = null
        )).emit()
        GroupState.flow().first { it.members.size == 2 }

        testScheduler.advanceTimeBy(15_001)
        testScheduler.runCurrent()
        val afterOwnExpiry = GroupState.flow().first { it.members.none { member -> member.user.id == me.id } }
        assertTrue(afterOwnExpiry.members.any { it.user.id == other.id })

        testScheduler.advanceTimeBy(45_001)
        testScheduler.runCurrent()
        assertTrue(GroupState.flow().first { it.members.isEmpty() }.members.isEmpty())
    }

    @Test
    fun `local expiry is exactly fifteen seconds and remote expiry remains one minute`() = runTest(
        Events() + States() + DatabaseBackgroundDispatcher(Dispatchers.Unconfined)
    ) {
        val clock = SchedulerClock(testScheduler)
        launchMeStateActor(userService, clock)
        launchGroupStateActor(userService, currentCoroutineContext(), clock)
        try {
        val me = userService.me()
        val ownSensor = HeartRateSensorId("local-clocked")
        userService.linkSensor(me, ownSensor)
        val other = io.sellmair.pacemaker.model.User(UserId(40), "Remote runner")
        userService.saveUser(other)
        val now = clock.now()
        HeartRateMeasurementEvent(HeartRate(111), ownSensor, now).emit()
        PacemakerBroadcastPackageEvent(PacemakerBroadcastPackage(
            receivedTime = now, deviceId = BleDeviceId("clocked-peer"), userId = other.id,
            userName = other.name, heartRate = HeartRate(151), heartRateLimit = HeartRate(175), userColorHue = null
        )).emit()
        testScheduler.runCurrent()
        GroupState.flow().first { it.members.size == 2 }

        testScheduler.advanceTimeBy(14_999)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.any { it.user.id == me.id })
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertTrue(GroupState.flow().first { it.members.none { member -> member.user.id == me.id } }
            .members.any { it.user.id == other.id })

        testScheduler.advanceTimeBy(44_999)
        testScheduler.runCurrent()
        assertTrue(GroupState.value().members.any { it.user.id == other.id })
        testScheduler.advanceTimeBy(1)
        testScheduler.runCurrent()
        assertEquals(GroupState.default, GroupState.flow().first { it == GroupState.default })
        } finally {
            currentCoroutineContext().cancelChildren()
        }
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(
        Events() + States() + DatabaseBackgroundDispatcher(Dispatchers.Unconfined)
    ) {
        try {
            launchMeStateActor(userService)
            launchGroupStateActor(userService, actorContext = currentCoroutineContext())
            block()
        } finally {
            currentCoroutineContext().cancelChildren()
        }
    }
}

private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock {
    private val epochMillis = 1_800_000_000_000L
    override fun now(): Instant = Instant.fromEpochMilliseconds(epochMillis + scheduler.currentTime)
}
