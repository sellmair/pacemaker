package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.sellmair.evas.compose.composeState
import io.sellmair.evas.compose.composeValue
import io.sellmair.evas.set
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.BluetoothState
import io.sellmair.pacemaker.HeartRateSensorConnectionState
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.SelectedHeartRateSourceState
import io.sellmair.pacemaker.UtteranceState
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.ui.MeColor
import io.sellmair.pacemaker.ui.widget.Launching
import io.sellmair.pacemaker.hasFreshHeartRate
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
internal fun MainPage() {
    val me = MeState.composeValue()
    val group = GroupState.composeValue()
    val selectedSource = SelectedHeartRateSourceState.composeValue().source
    val sensorConnection = selectedSource
        ?.takeIf { it.kind == HeartRateSource.Kind.BLE_SENSOR }
        ?.let { HeartRateSensorConnectionState.Key(HeartRateSensorId(it.id)).composeValue() }
    val bluetooth = BluetoothState.composeValue()
    val utteranceState by UtteranceState.composeState()
    val nowEpochMillis = liveNowEpochMillis()

    LivePageContent(
        state = LiveScreenContentState(
            me = me,
            group = group,
            selectedSource = resolveLiveHeartRateSource(
                source = selectedSource,
                connection = sensorConnection,
                bluetooth = bluetooth,
                me = me,
                nowEpochMillis = nowEpochMillis
            ),
            utteranceState = utteranceState,
            nowEpochMillis = nowEpochMillis
        ),
        identityColor = MeColor(),
        onUtteranceStateChanged = Launching { state ->
            UtteranceState.set(state)
        }
    )
}

/**
 * The timer invalidates the screen so idle readings expire. Every composition also samples the
 * clock: a packet received between ticks must never be compared with the previous tick's time.
 */
@Composable
internal fun liveNowEpochMillis(
    clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
): Long {
    var clockTickEpochMillis by remember { mutableLongStateOf(clock()) }
    val clockScope = rememberCoroutineScope()
    LifecycleResumeEffect(Unit) {
        clockTickEpochMillis = clock()
        val clockJob = clockScope.launch {
            while (true) {
                delay(1.seconds)
                clockTickEpochMillis = clock()
            }
        }
        onPauseOrDispose { clockJob.cancel() }
    }

    // Read the observable tick to keep periodic invalidation, then sample the actual current time.
    return clockTickEpochMillis.let { clock() }
}

/**
 * A persisted BLE availability is only a snapshot. Resolve it from the current connection actor so a
 * process-restored CONNECTING selection does not suppress readings after the sensor reconnects.
 * When the actor has not attached yet, a fresh MeState sample is evidence of an active source only
 * while Bluetooth is known enabled and permitted.
 * Companion transport gaps also keep the last reading for its normal grace period.
 */
internal fun resolveLiveHeartRateSource(
    source: HeartRateSource?,
    connection: HeartRateSensorConnectionState?,
    bluetooth: BluetoothState?,
    me: MeState?,
    nowEpochMillis: Long
): HeartRateSource? {
    val freshMeasurement = me?.takeIf { it.hasFreshHeartRate(nowEpochMillis) }
    if (source?.kind != HeartRateSource.Kind.BLE_SENSOR) {
        return if (source?.kind == HeartRateSource.Kind.COMPANION_WATCH && freshMeasurement != null &&
            source.availability in setOf(HeartRateSource.Availability.CONNECTING, HeartRateSource.Availability.DISCONNECTED)
        ) {
            source.copy(
                availability = HeartRateSource.Availability.AVAILABLE,
                heartRate = freshMeasurement.heartRate,
                measuredAtEpochMillis = freshMeasurement.measuredAtEpochMillis
            )
        } else source
    }
    val availability = when {
        bluetooth != null && !bluetooth.isBluetoothPermissionGranted -> HeartRateSource.Availability.PERMISSION_REQUIRED
        bluetooth != null && !bluetooth.isBluetoothEnabled -> HeartRateSource.Availability.DISCONNECTED
        connection?.connectIfPossible == false -> HeartRateSource.Availability.DISCONNECTED
        bluetooth?.isBluetoothEnabled == true && bluetooth.isBluetoothPermissionGranted && freshMeasurement != null ->
            HeartRateSource.Availability.AVAILABLE
        connection != null -> when (connection.connectionState) {
            BleConnectable.ConnectionState.Connected -> HeartRateSource.Availability.AVAILABLE
            BleConnectable.ConnectionState.Connecting -> HeartRateSource.Availability.CONNECTING
            BleConnectable.ConnectionState.Disconnected, null -> HeartRateSource.Availability.DISCONNECTED
        }
        else -> HeartRateSource.Availability.DISCONNECTED
    }

    return source.copy(
        availability = availability,
        heartRate = freshMeasurement?.heartRate,
        measuredAtEpochMillis = me?.measuredAtEpochMillis
    )
}
