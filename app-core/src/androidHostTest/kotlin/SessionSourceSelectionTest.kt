import io.sellmair.evas.*
import io.sellmair.pacemaker.*
import io.sellmair.pacemaker.bluetooth.HeartRateMeasurementEvent
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import utils.createInMemoryDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class SessionSourceSelectionTest {
    @Test
    fun `recording excludes this phone strap when wrist source is selected but keeps peer strap`() =
        runTest(Events() + States()) {
            val database = createInMemoryDatabase()
            val users = SqlUserService(database, UserId(1))
            val sessions = SqlSessionService(database)
            val me = users.me()
            val ownStrap = HeartRateSensorId("phone-strap")
            users.linkSensor(me, ownStrap)
            val peer = User(UserId(2), "Other runner")
            users.saveUser(peer)
            val peerStrap = HeartRateSensorId("peer-strap")
            users.linkSensor(peer, peerStrap)

            launchMeStateActor(users)
            launchSessionActor(users, sessions)
            MeState.flow().first { it != null }
            currentCoroutineContext().statesOrThrow.setState(
                SelectedHeartRateSourceState,
                SelectedHeartRateSourceState(HeartRateSource(
                    WATCH_COMPANION_SOURCE_ID, HeartRateSource.Kind.COMPANION_WATCH,
                    HeartRateSource.Availability.AVAILABLE
                ))
            )
            ActiveSessionIntent.Start.emit()
            ActiveSessionState.flow().first { it?.session != null }

            val measuredAt = Clock.System.now()
            HeartRateMeasurementEvent(HeartRate(122), ownStrap, measuredAt).emit()
            HeartRateMeasurementEvent(HeartRate(144), peerStrap, measuredAt).emit()
            testScheduler.runCurrent()

            val stored = sessions.getSessions().single()
            assertEquals(emptyList(), stored.getHeartRateMeasurements(me))
            assertEquals(1, stored.getHeartRateMeasurements(peer).size)

            currentCoroutineContext().cancelChildren()
        }
}
