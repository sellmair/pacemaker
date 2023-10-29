import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class HeartRateEncodingTest {
    @Test
    fun `test - 130`() {
        assertEquals(HeartRate(130), HeartRate(HeartRate(130).encodeToByteArray()))
    }

    @Test
    fun `test - 10`() {
        assertEquals(HeartRate(10f), HeartRate(HeartRate(10f).encodeToByteArray()))
    }
}