
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.SqlUserService
import io.sellmair.pacemaker.UserColors
import io.sellmair.pacemaker.UserService
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.launchGroupStateActor
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.utils.EventBus
import io.sellmair.pacemaker.utils.StateBus
import io.sellmair.pacemaker.utils.emit
import io.sellmair.pacemaker.utils.get
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

        launch { GroupState.get().collect { println("s: $it") } }

        val state = GroupState.get().first { it.members.isNotEmpty() }

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
            ), state
        )

        assertEquals(state, GroupState.get().value)
        GroupState.get().first { it == GroupState.default }

        assertEquals(
            GroupState.KeepMeasurementDuration.value().inWholeMilliseconds,
            testScheduler.currentTime
        )
    }


    @OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
    private fun test(block: suspend TestScope.() -> Unit) = runTest(EventBus() + StateBus()) {
        try {
            Dispatchers.setMain(currentCoroutineContext()[CoroutineDispatcher]!!)
            launchGroupStateActor(userService)
            block()
        } finally {
            currentCoroutineContext().cancelChildren()
            Dispatchers.resetMain()
        }
    }
}