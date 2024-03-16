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

@OptIn(ExperimentalCoroutinesApi::class)
class GroupStateTest {

    private val userService = SqlUserService(createInMemoryDatabase(), UserId(1))

    @Test
    fun `sample 0`() = test {
        val meSensorId = HeartRateSensorId("me")
        userService.linkSensor(userService.me(), meSensorId)
        HeartRateMeasurementEvent(HeartRate(128f), meSensorId, Clock.System.now()).emit()

        launch(StandardTestDispatcher(testScheduler), start = CoroutineStart.UNDISPATCHED) {
            GroupState.get().collect { println("(${testScheduler.currentTime}ms) s: $it") }
        }

        withContext(UnconfinedTestDispatcher(testScheduler)) {
            testScheduler.runCurrent()

            val state = GroupState.get().first { it.members.isNotEmpty() }
            println("(${testScheduler.currentTime}ms) Received first state with non-empty members")

            assertEquals(
                GroupState(
                    listOf(
                        UserState(
                            user = userService.me(),
                            isMe = true,
                            heartRate = HeartRate(128f),
                            heartRateLimit = UserService.NewUserHeartRateLimit.value(),
                            color = UserColors.default(userService.me().id)
                        )
                    )
                ), state,
                "Expect first state with non-empty members to reflect the previously emitted '${HeartRateMeasurementEvent::class}'"
            )

            println("(${testScheduler.currentTime}ms) Testing current state value...")
            assertEquals(
                state, GroupState.get().value,
                "(${testScheduler.currentTime}ms) Expect the current value of 'GroupState' to be the recently emitted state"
            )

            println("(${testScheduler.currentTime}ms) Waiting until the Group state resets..")
            GroupState.get().first { it == GroupState.default }

            println("(${testScheduler.currentTime}ms) Group state was reset!")
            assertEquals(
                GroupState.KeepMeasurementDuration.value().inWholeMilliseconds,
                testScheduler.currentTime,
                "Expect that the default value was emitted after the correct time"
            )
        }
    }


    @OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
    private fun test(block: suspend TestScope.() -> Unit) = runTest(EventBus() + StateBus()) {
        try {
            val main = StandardTestDispatcher(testScheduler, "main")
            Dispatchers.setMain(main)
            launchGroupStateActor(userService)
            block()
        } finally {
            currentCoroutineContext().cancelChildren()
            Dispatchers.resetMain()
        }
    }
}