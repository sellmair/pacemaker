import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.sellmair.pacemaker.SqliteUserService
import io.sellmair.pacemaker.UserService
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.invoke
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqliteUserServiceTest {

    private suspend fun service(): SqliteUserService {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        PacemakerDatabase.Schema.create(driver).await()
        val database = PacemakerDatabase(driver)
        return SqliteUserService(database)
    }

    @Test
    fun `test - me`() = runTest {
        val service = service()
        val me = service.me()
        assertEquals(me, service.me())
    }

    @Test
    fun `test - update my heart rate limit`() = runTest {
        val service = service()

        withContext(UserService.NewUserHeartRateLimit(HeartRate(100))) {
            val me = service.me()
            assertEquals(HeartRate(100), service.findHeartRateLimit(me))

            service.saveHeartRateLimit(me, HeartRate(160))
            assertEquals(HeartRate(160), service.findHeartRateLimit(me))
        }
    }

    @Test
    fun `test - save new user`() = runTest {
        val service = service()
        val user = User(id = UserId(5L), name = "foo", isAdhoc = true)
        service.saveUser(user)

        assertEquals(user, service.findUser(user.id))
        assertNull(service.findHeartRateLimit(user))
    }

    @Test
    fun `test - link sensor`() = runTest {
        val service = service()
        val me = service.me()
        val sensor = HeartRateSensorId("foo")

        service.linkSensor(me, sensor)
        assertEquals(me, service.findUser(sensor))

        val otherUser = User(UserId(2), "Seb")
        service.saveUser(otherUser)
        service.linkSensor(otherUser, sensor)

        assertEquals(otherUser, service.findUser(sensor))

        service.unlinkSensor(sensor)
        assertNull(service.findUser(sensor))
    }
}