package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.BluetoothState
import io.sellmair.pacemaker.HeartRateSensorConnectionState
import io.sellmair.pacemaker.HeartRateSensorState
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.WATCH_COMPANION_SOURCE_ID
import io.sellmair.pacemaker.WatchCompanionDeviceState
import io.sellmair.pacemaker.WatchCompanionCandidate
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.ui.PacemakerIcons
import io.sellmair.pacemaker.ui.PacemakerSpacing
import io.sellmair.pacemaker.ui.SharedRhythmColors
import io.sellmair.pacemaker.ui.widget.PacemakerDeviceCard
import io.sellmair.pacemaker.ui.widget.PacemakerStatusNotice
import io.sellmair.pacemaker.ui.widget.PacemakerNoticeTone
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.settings_bluetooth_off
import pacemaker.app.generated.resources.settings_bluetooth_off_title
import pacemaker.app.generated.resources.settings_bluetooth_permission
import pacemaker.app.generated.resources.settings_bluetooth_permission_title
import pacemaker.app.generated.resources.settings_bluetooth_unknown
import pacemaker.app.generated.resources.settings_bluetooth_unknown_title
import pacemaker.app.generated.resources.settings_connect_sensor
import pacemaker.app.generated.resources.settings_devices_description
import pacemaker.app.generated.resources.settings_devices_title
import pacemaker.app.generated.resources.settings_disconnect_sensor
import pacemaker.app.generated.resources.settings_eyebrow
import pacemaker.app.generated.resources.settings_searching_sensors
import pacemaker.app.generated.resources.settings_searching_title
import pacemaker.app.generated.resources.settings_select_sensor
import pacemaker.app.generated.resources.settings_sensor_available
import pacemaker.app.generated.resources.settings_sensor_connected
import pacemaker.app.generated.resources.settings_sensor_connecting
import pacemaker.app.generated.resources.settings_sensor_disconnecting
import pacemaker.app.generated.resources.settings_sensor_offline
import pacemaker.app.generated.resources.settings_sensor_permission
import pacemaker.app.generated.resources.settings_source_automatic
import pacemaker.app.generated.resources.settings_source_sensor
import pacemaker.app.generated.resources.settings_source_watch
import pacemaker.app.generated.resources.settings_watch_connected
import pacemaker.app.generated.resources.settings_watch_not_paired
import pacemaker.app.generated.resources.settings_watch_offline
import pacemaker.app.generated.resources.settings_watch_unavailable
import pacemaker.app.generated.resources.use_watch_source
import pacemaker.app.generated.resources.nearby_heart_rate_sensors
import pacemaker.app.generated.resources.live_source_automatic_footer
import pacemaker.app.generated.resources.live_source_footer
import pacemaker.app.generated.resources.live_source_ble
import pacemaker.app.generated.resources.live_source_watch
import pacemaker.app.generated.resources.live_source_available
import pacemaker.app.generated.resources.live_source_connecting
import pacemaker.app.generated.resources.live_source_offline
import pacemaker.app.generated.resources.live_source_permission
import pacemaker.app.generated.resources.live_source_unavailable

data class DeviceSensorContentState(
    val sensor: HeartRateSensorState,
    val connection: HeartRateSensorConnectionState?
)

data class DevicesScreenContentState(
    val selectedSource: HeartRateSource?,
    val sensors: List<DeviceSensorContentState>,
    val companion: WatchCompanionDeviceState,
    val bluetooth: BluetoothState? = null
)

@Composable
fun DevicesPageContent(
    state: DevicesScreenContentState,
    onSourceSelected: (HeartRateSource?) -> Unit,
    onSensorConnect: (HeartRateSensorId) -> Unit,
    onSensorDisconnect: (HeartRateSensorId) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SharedRhythmColors()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x4)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x2)) {
            Text(
                stringResource(Res.string.settings_eyebrow),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted
            )
            Text(
                stringResource(Res.string.settings_devices_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 40.sp, lineHeight = 42.sp),
                color = colors.text
            )
            Text(
                stringResource(Res.string.settings_devices_description),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted
            )
        }

        Row(
            Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(colors.surface)
                .border(1.dp, colors.border, MaterialTheme.shapes.small)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SourceChoice(
                label = stringResource(Res.string.settings_source_automatic),
                selected = state.selectedSource == null,
                modifier = Modifier.weight(1f)
            ) { onSourceSelected(null) }
            SourceChoice(
                label = stringResource(Res.string.settings_source_sensor),
                selected = state.selectedSource?.kind == HeartRateSource.Kind.BLE_SENSOR,
                modifier = Modifier.weight(1f)
            ) {
                val current = state.selectedSource?.takeIf { it.kind == HeartRateSource.Kind.BLE_SENSOR }
                state.sensors.firstOrNull()?.let { sensor ->
                    val matchingSensor = state.sensors.firstOrNull { it.sensor.id.value == current?.id }
                    when {
                        matchingSensor != null -> onSourceSelected(matchingSensor.toHeartRateSource(state.bluetooth))
                        current != null -> onSourceSelected(current)
                        else -> onSourceSelected(sensor.toHeartRateSource(state.bluetooth))
                    }
                }
                if (current != null && state.sensors.isEmpty()) onSourceSelected(current)
            }
            SourceChoice(
                label = stringResource(Res.string.settings_source_watch),
                selected = state.selectedSource?.kind == HeartRateSource.Kind.COMPANION_WATCH,
                modifier = Modifier.weight(1f),
                enabled = state.companion.platformAvailable
            ) {
                if (state.companion.platformAvailable) {
                    val current = state.selectedSource?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }
                    val candidate = if (current != null) {
                        state.companion.candidates.firstOrNull { it.id == current.companionDeviceId }
                    } else state.companion.candidates.firstOrNull()
                    if (candidate != null) onSourceSelected(candidate.toHeartRateSource())
                    else if (current != null) onSourceSelected(current)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x3)) {
            WatchCards(
                selectedSource = state.selectedSource,
                companion = state.companion,
                onSourceSelected = onSourceSelected
            )

            state.sensors.forEach { sensor ->
                val sensorDetail = sensor.statusLabel(state.bluetooth)
                val selected = state.selectedSource?.kind == HeartRateSource.Kind.BLE_SENSOR &&
                    state.selectedSource.id == sensor.sensor.id.value
                val connection = sensor.connection
                val connectionState = connection?.connectionState
                PacemakerDeviceCard(
                    title = sensor.sensor.name?.takeUnless(String::isBlank)
                        ?: stringResource(Res.string.settings_source_sensor),
                    detail = sensorDetail,
                    icon = PacemakerIcons.Sensor,
                    selected = selected,
                    actionLabel = stringResource(Res.string.settings_select_sensor),
                    onAction = { onSourceSelected(sensor.toHeartRateSource(state.bluetooth)) },
                    enabled = true,
                    secondaryActionLabel = when {
                        connection != null && connectionState == null -> stringResource(Res.string.settings_sensor_disconnecting)
                        connectionState == BleConnectable.ConnectionState.Connected -> stringResource(Res.string.settings_disconnect_sensor)
                        connectionState == BleConnectable.ConnectionState.Connecting -> null
                        else -> stringResource(Res.string.settings_connect_sensor)
                    },
                    onSecondaryAction = when {
                        connection != null && connectionState == null -> null
                        connectionState == BleConnectable.ConnectionState.Connected -> ({ onSensorDisconnect(sensor.sensor.id) })
                        connectionState == BleConnectable.ConnectionState.Connecting -> null
                        else -> ({ onSensorConnect(sensor.sensor.id) })
                    },
                    secondaryActionEnabled = connectionState == BleConnectable.ConnectionState.Connected ||
                        state.bluetooth?.let {
                            it.isBluetoothPermissionGranted && it.isBluetoothEnabled
                        } ?: true
                )
            }

            if (state.selectedSource == null) {
                Text(
                    stringResource(Res.string.live_source_automatic_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted
                )
            } else {
                SourceFooter(state.selectedSource, state)
            }

            Text(
                stringResource(Res.string.nearby_heart_rate_sensors).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.muted
            )

            BluetoothAvailabilityNotice(state.bluetooth, state.sensors.isEmpty())
        }
    }
}

@Composable
private fun SourceChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = SharedRhythmColors()
    Box(
        modifier.heightIn(min = 40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) SharedRhythmColors.Acid else androidx.compose.ui.graphics.Color.Transparent)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
            }
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) SharedRhythmColors.Charcoal else colors.text,
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun WatchCards(
    selectedSource: HeartRateSource?,
    companion: WatchCompanionDeviceState,
    onSourceSelected: (HeartRateSource?) -> Unit
) {
    val selectedWatch = selectedSource?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }
    val hasSelectedCandidate = companion.candidates.any { it.id == selectedWatch?.companionDeviceId }
    companion.candidates.forEach { candidate ->
        val selected = selectedWatch?.companionDeviceId == candidate.id
        val detail = when {
            !companion.platformAvailable -> stringResource(Res.string.settings_watch_unavailable)
            candidate.connected -> stringResource(Res.string.settings_watch_connected)
            candidate.paired -> stringResource(Res.string.settings_watch_offline)
            else -> stringResource(Res.string.settings_watch_not_paired)
        }
        PacemakerDeviceCard(
            title = candidate.name,
            detail = detail,
            icon = PacemakerIcons.Watch,
            selected = selected,
            actionLabel = stringResource(Res.string.use_watch_source),
            onAction = { onSourceSelected(candidate.toHeartRateSource()) },
            enabled = companion.platformAvailable
        )
    }

    if (selectedWatch != null && !hasSelectedCandidate) {
        PacemakerDeviceCard(
            title = selectedWatch.connectionName?.takeUnless(String::isBlank)
                ?: stringResource(Res.string.settings_source_watch),
            detail = stringResource(Res.string.settings_sensor_offline),
            icon = PacemakerIcons.Watch,
            selected = true,
            actionLabel = stringResource(Res.string.use_watch_source),
            onAction = { onSourceSelected(selectedWatch) }
        )
    } else if (companion.candidates.isEmpty()) {
        Text(
            stringResource(
                if (companion.platformAvailable) Res.string.settings_watch_not_paired
                else Res.string.settings_watch_unavailable
            ),
            style = MaterialTheme.typography.bodySmall,
            color = SharedRhythmColors().muted
        )
    }
}

@Composable
private fun DeviceSensorContentState.statusLabel(bluetooth: BluetoothState?): String = when {
    bluetooth != null && !bluetooth.isBluetoothPermissionGranted -> stringResource(Res.string.settings_sensor_permission)
    bluetooth != null && !bluetooth.isBluetoothEnabled -> stringResource(Res.string.settings_sensor_offline)
    connection == null -> stringResource(Res.string.settings_sensor_available)
    connection.connectionState == BleConnectable.ConnectionState.Connected -> stringResource(Res.string.settings_sensor_connected)
    connection.connectionState == BleConnectable.ConnectionState.Connecting -> stringResource(Res.string.settings_sensor_connecting)
    connection.connectionState == null -> stringResource(Res.string.settings_sensor_disconnecting)
    else -> stringResource(Res.string.settings_sensor_offline)
}

@Composable
private fun BluetoothAvailabilityNotice(bluetooth: BluetoothState?, noSensors: Boolean) {
    val isBlocked = bluetooth != null &&
        (!bluetooth.isBluetoothPermissionGranted || !bluetooth.isBluetoothEnabled)
    if (!isBlocked && !noSensors) return
    val (title, message) = when {
        bluetooth == null -> Res.string.settings_bluetooth_unknown_title to Res.string.settings_bluetooth_unknown
        !bluetooth.isBluetoothPermissionGranted -> Res.string.settings_bluetooth_permission_title to Res.string.settings_bluetooth_permission
        !bluetooth.isBluetoothEnabled -> Res.string.settings_bluetooth_off_title to Res.string.settings_bluetooth_off
        else -> Res.string.settings_searching_title to Res.string.settings_searching_sensors
    }
    PacemakerStatusNotice(
        title = stringResource(title),
        message = stringResource(message),
        tone = if (isBlocked) PacemakerNoticeTone.Attention else PacemakerNoticeTone.Information
    )
}

@Composable
private fun SourceFooter(source: HeartRateSource, state: DevicesScreenContentState) {
    val name = source.connectionName?.takeUnless(String::isBlank) ?: stringResource(
        if (source.kind == HeartRateSource.Kind.COMPANION_WATCH) Res.string.live_source_watch else Res.string.live_source_ble
    )
    val availability = when (source.kind) {
        HeartRateSource.Kind.BLE_SENSOR -> state.sensors.firstOrNull { it.sensor.id.value == source.id }
            ?.availability(state.bluetooth) ?: if (state.bluetooth?.isBluetoothPermissionGranted == false) {
                HeartRateSource.Availability.PERMISSION_REQUIRED
            } else {
                HeartRateSource.Availability.DISCONNECTED
            }
        HeartRateSource.Kind.COMPANION_WATCH -> when {
            !state.companion.platformAvailable -> HeartRateSource.Availability.UNSUPPORTED
            state.companion.candidates.any { it.id == source.companionDeviceId && it.connected } ->
                HeartRateSource.Availability.AVAILABLE
            else -> HeartRateSource.Availability.DISCONNECTED
        }
        HeartRateSource.Kind.ONBOARD_WATCH -> source.availability
    }
    val status = stringResource(
        when (availability) {
            HeartRateSource.Availability.AVAILABLE -> Res.string.live_source_available
            HeartRateSource.Availability.CONNECTING -> Res.string.live_source_connecting
            HeartRateSource.Availability.DISCONNECTED -> Res.string.live_source_offline
            HeartRateSource.Availability.PERMISSION_REQUIRED -> Res.string.live_source_permission
            HeartRateSource.Availability.UNSUPPORTED -> Res.string.live_source_unavailable
        }
    )
    Text(
        stringResource(Res.string.live_source_footer, name, status),
        style = MaterialTheme.typography.bodySmall,
        color = SharedRhythmColors().muted
    )
}

private fun DeviceSensorContentState.availability(bluetooth: BluetoothState?): HeartRateSource.Availability = when {
    bluetooth != null && !bluetooth.isBluetoothPermissionGranted -> HeartRateSource.Availability.PERMISSION_REQUIRED
    bluetooth != null && !bluetooth.isBluetoothEnabled -> HeartRateSource.Availability.DISCONNECTED
    connection?.connectionState == BleConnectable.ConnectionState.Connected -> HeartRateSource.Availability.AVAILABLE
    connection?.connectionState == BleConnectable.ConnectionState.Connecting -> HeartRateSource.Availability.CONNECTING
    else -> HeartRateSource.Availability.DISCONNECTED
}

private fun DeviceSensorContentState.toHeartRateSource(bluetooth: BluetoothState?) = HeartRateSource(
    id = sensor.id.value,
    kind = HeartRateSource.Kind.BLE_SENSOR,
    availability = availability(bluetooth),
    connectionName = sensor.name
)

private fun WatchCompanionCandidate.toHeartRateSource() = HeartRateSource(
    id = WATCH_COMPANION_SOURCE_ID,
    kind = HeartRateSource.Kind.COMPANION_WATCH,
    availability = if (connected) HeartRateSource.Availability.AVAILABLE else HeartRateSource.Availability.DISCONNECTED,
    connectionName = name,
    companionDeviceId = id
)
