@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.sellmair.pacemaker

import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.mainPage.resolveSelectedBleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Uses the production boundary resolver without widening its internal visibility. */
class SharedRhythmBlePresentationTest {
    private val now = 1_800_000_000_000L
    private val me = MeState(User(UserId(1), "Sebastian"), HeartRate(142), HeartRate(150), now)
    private val enabled = BluetoothState(true, true)
    private val restored = HeartRateSource(
        "strap",
        HeartRateSource.Kind.BLE_SENSOR,
        HeartRateSource.Availability.CONNECTING
    )

    @Test
    fun restoredConnectingChoiceUsesRuntimeConnectedStateAndFreshMeasurement() {
        val result = resolveSelectedBleSource(
            restored,
            HeartRateSensorConnectionState(true, BleConnectable.ConnectionState.Connected),
            enabled,
            me,
            now
        )
        assertEquals(HeartRateSource.Availability.AVAILABLE, result?.availability)
        assertEquals(HeartRate(142), result?.currentHeartRate(now))
    }

    @Test
    fun disconnectingAndDisabledBluetoothSuppressOldFreshMeasurement() {
        val disconnecting = resolveSelectedBleSource(
            restored,
            HeartRateSensorConnectionState(false, null),
            enabled,
            me,
            now
        )
        assertEquals(HeartRateSource.Availability.DISCONNECTED, disconnecting?.availability)
        assertNull(disconnecting?.currentHeartRate(now))
        val disabled = resolveSelectedBleSource(
            restored,
            HeartRateSensorConnectionState(true, BleConnectable.ConnectionState.Connected),
            BluetoothState(false, true),
            me,
            now
        )
        assertNull(disabled?.currentHeartRate(now))
    }

    @Test
    fun absentActorFallbackRequiresEnabledBluetoothAndFreshMeasurement() {
        assertEquals(
            HeartRate(142),
            resolveSelectedBleSource(restored, null, enabled, me, now)?.currentHeartRate(now)
        )
        assertNull(resolveSelectedBleSource(restored, null, null, me, now)?.currentHeartRate(now))
        assertNull(
            resolveSelectedBleSource(
                restored,
                null,
                enabled,
                me,
                now + 15_001
            )?.currentHeartRate(now + 15_001)
        )
    }
}
