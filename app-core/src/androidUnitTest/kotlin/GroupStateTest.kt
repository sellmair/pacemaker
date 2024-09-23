import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.collect
import io.sellmair.evas.emit
import io.sellmair.evas.flow
import io.sellmair.evas.value
import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.datetime.Clock
import utils.createInMemoryDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class GroupStateTest {

    private val userService = SqlUserService(createInMemoryDatabase(), UserId(1))

    @Test
    fun `sample 0`() = test {
        val meSensorId = HeartRateSensorId("me")
        val me = userService.me()
        userService.linkSensor(me, meSensorId)
        HeartRateMeasurementEvent(HeartRate(128f), meSensorId, Clock.System.now()).emit()

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
                        color = UserColors.default(me.id)
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
        assertEquals(
            GroupState.KeepMeasurementDuration.value().inWholeMilliseconds,
            testScheduler.currentTime,
            "Expect that the default value was emitted after the correct time"
        )
    }

    private fun test(block: suspend TestScope.() -> Unit) = runTest(Events() + States()) {
        try {
            launchGroupStateActor(userService, actorContext = currentCoroutineContext())
            block()
        } finally {
            currentCoroutineContext().cancelChildren()
        }
    }
}
