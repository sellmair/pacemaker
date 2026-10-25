package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.GroupState
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.UtteranceState
import io.sellmair.pacemaker.UserState
import io.sellmair.pacemaker.model.nameAbbreviation
import io.sellmair.pacemaker.ui.PacemakerSpacing
import io.sellmair.pacemaker.ui.PacemakerIcons
import io.sellmair.pacemaker.ui.SharedRhythmColors
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.ui.widget.PacemakerMemberRow
import kotlin.math.ceil
import kotlin.math.floor
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.live_bpm
import pacemaker.app.generated.resources.live_group_count_with_you
import pacemaker.app.generated.resources.live_group_count_without_you
import pacemaker.app.generated.resources.live_group_empty
import pacemaker.app.generated.resources.live_together
import pacemaker.app.generated.resources.live_limit_description
import pacemaker.app.generated.resources.live_member_fallback
import pacemaker.app.generated.resources.live_member_invalid
import pacemaker.app.generated.resources.live_member_limit
import pacemaker.app.generated.resources.live_member_limit_missing
import pacemaker.app.generated.resources.live_member_limit_delta_below
import pacemaker.app.generated.resources.live_member_limit_delta_above
import pacemaker.app.generated.resources.live_member_below_limit
import pacemaker.app.generated.resources.live_member_above_limit
import pacemaker.app.generated.resources.live_member_at_limit
import pacemaker.app.generated.resources.live_member_outdated
import pacemaker.app.generated.resources.live_member_connection_lost
import pacemaker.app.generated.resources.live_no_current_reading
import pacemaker.app.generated.resources.live_meter_accessibility
import pacemaker.app.generated.resources.live_meter_start
import pacemaker.app.generated.resources.live_meter_end
import pacemaker.app.generated.resources.live_meter_limit_label
import pacemaker.app.generated.resources.live_meter_reading
import pacemaker.app.generated.resources.live_status_above
import pacemaker.app.generated.resources.live_status_connecting
import pacemaker.app.generated.resources.live_status_disconnected
import pacemaker.app.generated.resources.live_status_invalid
import pacemaker.app.generated.resources.live_status_live
import pacemaker.app.generated.resources.live_status_offline
import pacemaker.app.generated.resources.live_status_outdated
import pacemaker.app.generated.resources.live_status_permission
import pacemaker.app.generated.resources.live_status_signal_lost
import pacemaker.app.generated.resources.live_status_unsupported
import pacemaker.app.generated.resources.live_status_waiting
import pacemaker.app.generated.resources.live_status_waiting_badge
import pacemaker.app.generated.resources.live_status_within
import pacemaker.app.generated.resources.live_title
import pacemaker.app.generated.resources.live_you
import pacemaker.app.generated.resources.live_voice_all
import pacemaker.app.generated.resources.live_voice_button_all
import pacemaker.app.generated.resources.live_voice_button_off
import pacemaker.app.generated.resources.live_voice_button_warnings
import pacemaker.app.generated.resources.live_voice_off
import pacemaker.app.generated.resources.live_voice_warnings
import pacemaker.app.generated.resources.live_source_footer
import pacemaker.app.generated.resources.live_source_automatic_footer
import pacemaker.app.generated.resources.live_source_ble
import pacemaker.app.generated.resources.live_source_watch
import pacemaker.app.generated.resources.live_source_onboard_watch
import pacemaker.app.generated.resources.live_source_connected
import pacemaker.app.generated.resources.live_source_available
import pacemaker.app.generated.resources.live_source_connecting
import pacemaker.app.generated.resources.live_source_offline
import pacemaker.app.generated.resources.live_source_permission
import pacemaker.app.generated.resources.live_source_unavailable

/**
 * Real application models passed to a stateless presentation seam for focused UI tests.
 */
data class LiveScreenContentState(
    val me: MeState?,
    val group: GroupState?,
    val selectedSource: HeartRateSource?,
    val utteranceState: UtteranceState,
    val nowEpochMillis: Long
)

@Composable
fun LivePageContent(
    state: LiveScreenContentState,
    identityColor: Color,
    onUtteranceStateChanged: (UtteranceState) -> Unit
) {
    val colors = SharedRhythmColors()
    val me = state.me
    val groupMembers = state.group?.members.orEmpty()
    val currentUserId = me?.me?.id ?: groupMembers.firstOrNull { it.isMe }?.user?.id
    val groupSelf = groupMembers.firstOrNull { member ->
        currentUserId != null && member.user.id == currentUserId
    } ?: groupMembers.firstOrNull { it.isMe && me == null }
    val peers = groupMembers
        .filterNot { it.user.id == currentUserId }
        .distinctBy { it.user.id }

    val ownRate = if (me != null) me.heartRate else groupSelf?.heartRate
    val ownMeasuredAt = if (me != null) {
        me.measuredAtEpochMillis
    } else {
        groupSelf?.measuredAtEpochMillis
    }
    val sourceAllowsReading = state.selectedSource == null ||
            state.selectedSource.availability == HeartRateSource.Availability.AVAILABLE
    val ownFreshness = ownMeasuredAt.toFreshness(state.nowEpochMillis)
    val ownRateBpm = ownRate?.takeIf {
        ownFreshness == MeasurementFreshness.Fresh && sourceAllowsReading
    }?.toString()?.toIntOrNull()
    val ownLimit = me?.heartRateLimit ?: groupSelf?.heartRateLimit
    val ownLimitBpm = ownLimit?.toString()?.toIntOrNull()
    val aboveLimit = ownRateBpm != null && ownLimit != null && ownRate > ownLimit

    val statusTitle = when {
        ownRateBpm != null && aboveLimit -> stringResource(Res.string.live_status_above)
        ownRateBpm != null && ownLimit != null -> stringResource(Res.string.live_status_within)
        ownRateBpm != null -> stringResource(Res.string.live_status_live)
        state.selectedSource?.availability == HeartRateSource.Availability.DISCONNECTED ->
            stringResource(Res.string.live_status_signal_lost)

        ownFreshness == MeasurementFreshness.Expired || (ownRate != null && ownFreshness == MeasurementFreshness.Invalid) ->
            stringResource(Res.string.live_status_signal_lost)

        ownRate != null -> sourceStatus(state.selectedSource, state.nowEpochMillis)
        else -> sourceStatus(state.selectedSource, state.nowEpochMillis)
    }
    val liveHeaderLabel = stringResource(Res.string.live_status_live)
    val signalLostLabel = stringResource(Res.string.live_status_signal_lost)
    val headerStatusLabel = when {
        ownRateBpm != null -> liveHeaderLabel
        state.selectedSource?.availability == HeartRateSource.Availability.DISCONNECTED ->
            stringResource(Res.string.live_status_offline)
        else -> stringResource(Res.string.live_status_waiting_badge)
    }
    val headerFontScale = LocalDensity.current.fontScale

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x4)
    ) {
        val liveTitle = stringResource(Res.string.live_title).uppercase()
        val badgeColor = when {
            ownRateBpm != null -> colors.success.ink
            aboveLimit -> colors.danger.ink
            else -> colors.muted
        }
        if (headerFontScale >= 1.3f) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)
            ) {
                Text(
                    liveTitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                LiveHeaderStatus(headerStatusLabel, ownRateBpm != null, badgeColor)
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    liveTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                LiveHeaderStatus(headerStatusLabel, ownRateBpm != null, badgeColor)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val fontScale = LocalDensity.current.fontScale
                val metricFontSize = when {
                    fontScale >= 1.5f -> 64.sp
                    fontScale >= 1.3f -> 72.sp
                    fontScale >= 1.15f -> 88.sp
                    maxWidth < 320.dp -> 88.sp
                    maxWidth < 336.dp -> 100.sp
                    else -> 112.sp
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        ownRateBpm?.toString() ?: "—",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = metricFontSize,
                            lineHeight = (metricFontSize.value * .95f).sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = colors.text
                    )
                    Spacer(Modifier.width(PacemakerSpacing.x2))
                    Text(
                        stringResource(Res.string.live_bpm),
                        modifier = Modifier.padding(bottom = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.muted
                    )
                }
            }
            if (ownRateBpm == null || ownLimitBpm != null) {
                val statusColor = when {
                    aboveLimit -> colors.danger.ink
                    ownRateBpm != null && ownLimitBpm != null -> colors.success.ink
                    statusTitle == signalLostLabel -> colors.warning.ink
                    else -> colors.text
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        aboveLimit -> Icon(PacemakerIcons.Warning, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                        ownRateBpm != null && ownLimitBpm != null -> Icon(PacemakerIcons.Check, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                        statusTitle == signalLostLabel -> Icon(PacemakerIcons.Disconnect, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
                    }
                    Text(statusTitle, style = MaterialTheme.typography.titleMedium, color = statusColor)
                }
            }
        }

        HeartRateLimitMeter(
            currentBpm = ownRateBpm,
            limitBpm = ownLimitBpm,
            aboveLimit = aboveLimit,
            accessibilityLabel = stringResource(Res.string.live_meter_accessibility),
            limitLabel = ownLimitBpm?.let { stringResource(Res.string.live_limit_description, it) },
            visualLimitLabel = ownLimitBpm?.let { stringResource(Res.string.live_meter_limit_label, it) }
        )

        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(PacemakerSpacing.x1)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.live_together).uppercase(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    ),
                    color = colors.text
                )
                Text(
                    stringResource(
                        if (me != null || groupSelf != null) Res.string.live_group_count_with_you
                        else Res.string.live_group_count_without_you,
                        peers.size + if (me != null || groupSelf != null) 1 else 0
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.muted
                )
            }
            if (me != null) {
                LiveMemberRow(
                    name = stringResource(Res.string.live_you),
                    initials = me.me.nameAbbreviation.ifBlank { "?" }.uppercase(),
                    heartRateBpm = ownRateBpm,
                    freshness = ownFreshness,
                    limitBpm = ownLimitBpm,
                    identityColor = identityColor,
                    readingStatusOverride = if (ownRateBpm == null) statusTitle else null,
                    isCurrentUser = true
                )
            } else if (groupSelf != null) {
                LiveMemberRow(
                    name = stringResource(Res.string.live_you),
                    initials = groupSelf.user.nameAbbreviation.ifBlank { "?" }.uppercase(),
                    heartRateBpm = groupSelf.heartRate.toString().toIntOrNull().takeIf {
                        groupSelf.freshness(state.nowEpochMillis) == MeasurementFreshness.Fresh && sourceAllowsReading
                    },
                    freshness = groupSelf.freshness(state.nowEpochMillis),
                    limitBpm = groupSelf.heartRateLimit?.toString()?.toIntOrNull(),
                    identityColor = groupSelf.color.toColor(),
                    readingStatusOverride = if (!sourceAllowsReading) statusTitle else null,
                    isCurrentUser = true
                )
            }
            AnimatedPeerRows(peers) { member ->
                LiveMemberRow(
                    name = member.user.name.ifBlank { stringResource(Res.string.live_member_fallback) },
                    initials = member.user.nameAbbreviation.ifBlank { "?" }.uppercase(),
                    heartRateBpm = member.heartRate.toString().toIntOrNull()
                        .takeIf { member.freshness(state.nowEpochMillis) == MeasurementFreshness.Fresh },
                    freshness = member.freshness(state.nowEpochMillis),
                    limitBpm = member.heartRateLimit?.toString()?.toIntOrNull(),
                    identityColor = member.color.toColor(),
                    readingStatusOverride = if (member.isConnectionLost || member.freshness(state.nowEpochMillis) == MeasurementFreshness.Expired) {
                        stringResource(Res.string.live_member_connection_lost)
                    } else null,
                    isCurrentUser = false
                )
            }
            if (me == null && groupSelf == null && peers.isEmpty()) {
                Text(stringResource(Res.string.live_group_empty), color = colors.muted)
            }
        }

        VoiceAlertControl(state = state.utteranceState, onStateSelected = onUtteranceStateChanged)
        LiveSourceFooter(source = state.selectedSource, nowEpochMillis = state.nowEpochMillis)
    }

}

/** Keep each departing identity composed until its fade and height collapse have finished. */
@Composable
private fun AnimatedPeerRows(members: List<UserState>, content: @Composable (UserState) -> Unit) {
    var retained by remember { mutableStateOf(members) }
    LaunchedEffect(members) {
        val currentById = members.associateBy { it.user.id }
        val retainedIds = retained.map { it.user.id }.toSet()
        retained = retained.map { currentById[it.user.id] ?: it } + members.filterNot { it.user.id in retainedIds }
    }
    Column {
        retained.forEach { previous ->
            key(previous.user.id) {
                val current = members.firstOrNull { it.user.id == previous.user.id }
                val visibility = remember { MutableTransitionState(true) }
                LaunchedEffect(current != null) { visibility.targetState = current != null }
                LaunchedEffect(visibility.isIdle, visibility.currentState, current != null) {
                    if (visibility.isIdle && !visibility.currentState && current == null) {
                        retained = retained.filterNot { it.user.id == previous.user.id }
                    }
                }
                AnimatedVisibility(
                    visibleState = visibility,
                    exit = fadeOut(tween(300)) + shrinkVertically(tween(300), shrinkTowards = Alignment.Top)
                ) {
                    Column {
                        content(current ?: previous)
                        Spacer(Modifier.height(PacemakerSpacing.x1))
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveHeaderStatus(label: String, isLive: Boolean, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLive) {
            Spacer(
                Modifier.width(8.dp).height(8.dp).clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(PacemakerSpacing.x1))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun sourceStatus(source: HeartRateSource?, nowEpochMillis: Long): String {
    if (source != null) {
        when (source.availability) {
            HeartRateSource.Availability.CONNECTING -> {
                val name = source.connectionName
                return if (name.isNullOrBlank()) {
                    stringResource(Res.string.live_status_waiting)
                } else {
                    stringResource(Res.string.live_status_connecting, name)
                }
            }

            HeartRateSource.Availability.DISCONNECTED ->
                return stringResource(Res.string.live_status_disconnected)

            HeartRateSource.Availability.PERMISSION_REQUIRED ->
                return stringResource(Res.string.live_status_permission)

            HeartRateSource.Availability.UNSUPPORTED ->
                return stringResource(Res.string.live_status_unsupported)

            HeartRateSource.Availability.AVAILABLE -> Unit
        }

        if (source.heartRate != null) {
            return when (source.measuredAtEpochMillis.toFreshness(nowEpochMillis)) {
                MeasurementFreshness.Expired -> stringResource(Res.string.live_status_outdated)
                MeasurementFreshness.Invalid -> stringResource(Res.string.live_status_invalid)
                MeasurementFreshness.Fresh -> stringResource(Res.string.live_status_waiting)
            }
        }
    }
    return stringResource(Res.string.live_status_waiting)
}

private enum class MeasurementFreshness { Fresh, Expired, Invalid }

private fun Long?.toFreshness(nowEpochMillis: Long): MeasurementFreshness {
    val measuredAt = this ?: return MeasurementFreshness.Invalid
    val age = nowEpochMillis - measuredAt
    return when {
        age < 0L -> MeasurementFreshness.Invalid
        age > HeartRateSource.MAX_AGE.inWholeMilliseconds -> MeasurementFreshness.Expired
        else -> MeasurementFreshness.Fresh
    }
}

private fun UserState.freshness(nowEpochMillis: Long): MeasurementFreshness =
    if (isConnectionLost) MeasurementFreshness.Expired else measuredAtEpochMillis.toFreshness(nowEpochMillis)

@Composable
private fun LiveMemberRow(
    name: String,
    initials: String,
    heartRateBpm: Int?,
    freshness: MeasurementFreshness,
    limitBpm: Int?,
    identityColor: Color,
    readingStatusOverride: String? = null,
    isCurrentUser: Boolean
) {
    val readingStatus = readingStatusOverride ?: when (freshness) {
        MeasurementFreshness.Fresh -> null
        MeasurementFreshness.Expired -> stringResource(Res.string.live_member_outdated)
        MeasurementFreshness.Invalid -> stringResource(Res.string.live_member_invalid)
    }
    val limitLabel = when {
        heartRateBpm != null && limitBpm != null && heartRateBpm == limitBpm ->
            if (isCurrentUser) stringResource(Res.string.live_member_at_limit)
            else stringResource(Res.string.live_member_limit, limitBpm)
        heartRateBpm != null && limitBpm != null && heartRateBpm < limitBpm ->
            if (isCurrentUser) stringResource(Res.string.live_member_below_limit, limitBpm - heartRateBpm)
            else stringResource(Res.string.live_member_limit_delta_below, limitBpm, limitBpm - heartRateBpm)
        heartRateBpm != null && limitBpm != null ->
            if (isCurrentUser) stringResource(Res.string.live_member_above_limit, heartRateBpm - limitBpm)
            else stringResource(Res.string.live_member_limit_delta_above, limitBpm, heartRateBpm - limitBpm)
        limitBpm != null -> stringResource(Res.string.live_member_limit, limitBpm)
        else -> stringResource(Res.string.live_member_limit_missing)
    }
    val colors = SharedRhythmColors()
    PacemakerMemberRow(
        name = name,
        initials = initials,
        heartRateBpm = heartRateBpm,
        limitBpm = limitBpm,
        identityColor = identityColor,
        limitLabel = limitLabel,
        readingStatus = readingStatus,
        limitLabelColor = if (heartRateBpm != null && limitBpm != null && heartRateBpm > limitBpm) {
            colors.danger.ink
        } else {
            null
        }
    )
}

@Composable
private fun HeartRateLimitMeter(
    currentBpm: Int?,
    limitBpm: Int?,
    aboveLimit: Boolean,
    accessibilityLabel: String,
    limitLabel: String?,
    visualLimitLabel: String?
) {
    val colors = SharedRhythmColors()
    val lower = minOf(60, currentBpm ?: 60, limitBpm ?: 60)
    val upper = maxOf(180, currentBpm ?: 180, limitBpm ?: 180)
    val domainStart = floor(lower / 10f) * 10f
    val domainEnd = ceil(upper / 10f) * 10f
    val description = listOfNotNull(accessibilityLabel, limitLabel).joinToString(", ")
    val currentReadingDescription = currentBpm?.let {
        stringResource(Res.string.live_meter_reading, it)
    } ?: stringResource(Res.string.live_no_current_reading)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stackLimitLabel = LocalDensity.current.fontScale >= 1.2f || maxWidth < 330.dp
        Column {
            Canvas(
                Modifier.fillMaxWidth()
                    .height(32.dp)
                    .semantics {
                        contentDescription = description
                        stateDescription = currentReadingDescription
                    }
            ) {
                val left = 0f
                val right = size.width
                val centerY = size.height / 2f
                val stroke = 8.dp.toPx()
                val limitX = limitBpm?.toMeterX(domainStart, domainEnd, left, right)
                val currentX = currentBpm?.toMeterX(domainStart, domainEnd, left, right)
                drawRect(colors.raised, Offset(left, centerY - stroke / 2f), androidx.compose.ui.geometry.Size(right - left, stroke))
                if (currentX != null) {
                    drawRect(
                        if (aboveLimit) colors.danger.ink else colors.accentText,
                        Offset(left, centerY - stroke / 2f),
                        androidx.compose.ui.geometry.Size(currentX - left, stroke)
                    )
                }
                if (limitX != null) {
                    drawLine(
                        colors.text,
                        Offset(limitX, centerY - 12.dp.toPx()),
                        Offset(limitX, centerY + 12.dp.toPx()),
                        2.dp.toPx()
                    )
                }
            }
            if (limitLabel != null || currentBpm != null) {
                val startLabel = stringResource(Res.string.live_meter_start, domainStart.toInt())
                val endLabel = stringResource(Res.string.live_meter_end, domainEnd.toInt())
                if (stackLimitLabel) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(startLabel, style = MaterialTheme.typography.labelSmall, color = colors.muted)
                        Text(endLabel, style = MaterialTheme.typography.labelSmall, color = colors.muted, textAlign = TextAlign.End)
                    }
                    visualLimitLabel?.let {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.text,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Text(startLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = colors.muted)
                        Row(
                            modifier = Modifier.weight(2f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            visualLimitLabel?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = colors.text, textAlign = TextAlign.Center)
                            }
                        }
                        Text(endLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = colors.muted, textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

private fun Int.toMeterX(start: Float, end: Float, left: Float, right: Float): Float {
    if (end <= start) return (left + right) / 2f
    val fraction = ((this - start) / (end - start)).coerceIn(0f, 1f)
    return left + (right - left) * fraction
}

@Composable
private fun VoiceAlertControl(
    state: UtteranceState,
    onStateSelected: (UtteranceState) -> Unit
) {
    val colors = SharedRhythmColors()
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        Box {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                onClick = { expanded = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (state == UtteranceState.Silence) PacemakerIcons.AudioOff else PacemakerIcons.Audio,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(PacemakerSpacing.x2))
                Text(
                    stringResource(
                        when (state) {
                            UtteranceState.Silence -> Res.string.live_voice_button_off
                            UtteranceState.Warnings -> Res.string.live_voice_button_warnings
                            UtteranceState.All -> Res.string.live_voice_button_all
                        }
                    ),
                    color = colors.text
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                UtteranceState.entries.forEach { option ->
                    val optionLabel = stringResource(
                        when (option) {
                            UtteranceState.Silence -> Res.string.live_voice_off
                            UtteranceState.Warnings -> Res.string.live_voice_warnings
                            UtteranceState.All -> Res.string.live_voice_all
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(optionLabel, color = colors.text) },
                        onClick = {
                            expanded = false
                            onStateSelected(option)
                        },
                        modifier = if (option == state) Modifier.semantics {
                            selected = true
                            stateDescription = optionLabel
                        } else Modifier
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveSourceFooter(source: HeartRateSource?, nowEpochMillis: Long) {
    val colors = SharedRhythmColors()
    if (source == null) {
        Text(
            stringResource(Res.string.live_source_automatic_footer),
            style = MaterialTheme.typography.bodySmall,
            color = colors.muted
        )
        return
    }
    val sourceName = source.connectionName?.takeUnless(String::isBlank) ?: stringResource(
        when (source.kind) {
            HeartRateSource.Kind.BLE_SENSOR -> Res.string.live_source_ble
            HeartRateSource.Kind.COMPANION_WATCH -> Res.string.live_source_watch
            HeartRateSource.Kind.ONBOARD_WATCH -> Res.string.live_source_onboard_watch
        }
    )
    val sourceState = stringResource(
        when (source.availability) {
            HeartRateSource.Availability.AVAILABLE -> if (source.currentHeartRate(nowEpochMillis) != null) {
                Res.string.live_source_connected
            } else {
                Res.string.live_source_available
            }
            HeartRateSource.Availability.CONNECTING -> Res.string.live_source_connecting
            HeartRateSource.Availability.DISCONNECTED -> Res.string.live_source_offline
            HeartRateSource.Availability.PERMISSION_REQUIRED -> Res.string.live_source_permission
            HeartRateSource.Availability.UNSUPPORTED -> Res.string.live_source_unavailable
        }
    )
    Text(
        stringResource(Res.string.live_source_footer, sourceName, sourceState),
        style = MaterialTheme.typography.bodySmall,
        color = colors.muted
    )
}
