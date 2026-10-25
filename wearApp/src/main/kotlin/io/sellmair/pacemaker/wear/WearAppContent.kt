package io.sellmair.pacemaker.wear

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalScrollCaptureInProgress
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.foundation.BasicSwipeToDismissBox
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class WearAppRoute {
    HOME,
    GROUP,
}

@Composable
internal fun rememberInteractiveWearNowEpochMillis(ambient: Boolean): Long {
    val lifecycleOwner = LocalLifecycleOwner.current
    var nowEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lifecycleOwner, ambient) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            if (!ambient) {
                while (isActive) {
                    nowEpochMillis = System.currentTimeMillis()
                    delay(1_000)
                }
            }
        }
    }

    return nowEpochMillis
}

@Composable
fun WearAppContent(
    state: WearWorkoutState,
    nowEpochMillis: Long,
    ambient: Boolean,
    onToggleWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WearSharedRhythmTheme {
        var route by rememberSaveable { mutableStateOf(WearAppRoute.HOME) }
        val homeListState = rememberTransformingLazyColumnState()
        val groupListState = rememberTransformingLazyColumnState()
        val swipeState = rememberSwipeToDismissBoxState()
        BackHandler(enabled = route == WearAppRoute.GROUP) {
            route = WearAppRoute.HOME
        }

        val colors = MaterialTheme.colorScheme
        AppScaffold(
            modifier = modifier.fillMaxSize(),
            timeText = {},
            containerColor = if (ambient) Color.Black else colors.background,
            contentColor = colors.onBackground,
        ) {
            if (ambient) {
                WearAmbientContent(state = state, modifier = Modifier.fillMaxSize())
            } else {
                BasicSwipeToDismissBox(
                    onDismissed = { route = WearAppRoute.HOME },
                    state = swipeState,
                    modifier = Modifier.fillMaxSize(),
                    backgroundKey = WearAppRoute.HOME,
                    contentKey = route,
                    userSwipeEnabled = route == WearAppRoute.GROUP,
                ) { isBackground ->
                    when (if (isBackground) WearAppRoute.HOME else route) {
                        WearAppRoute.HOME -> WearHomeContent(
                            state = state,
                            nowEpochMillis = nowEpochMillis,
                            ambient = false,
                            onToggleWorkout = onToggleWorkout,
                            onOpenGroup = { route = WearAppRoute.GROUP },
                            listState = homeListState,
                            modifier = Modifier.fillMaxSize(),
                        )

                        WearAppRoute.GROUP -> WearGroupContent(
                            state = state,
                            nowEpochMillis = nowEpochMillis,
                            onClose = { route = WearAppRoute.HOME },
                            listState = groupListState,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun WearHomeContent(
    state: WearWorkoutState,
    nowEpochMillis: Long,
    ambient: Boolean,
    onToggleWorkout: () -> Unit,
    onOpenGroup: () -> Unit,
    modifier: Modifier = Modifier,
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
) {
    if (ambient) {
        WearAmbientContent(state = state, modifier = modifier)
        return
    }

    val presentation = remember(state, nowEpochMillis) {
        state.toWearHomePresentation(nowEpochMillis)
    }
    val colors = MaterialTheme.colorScheme
    val transformationSpec = rememberTransformationSpec()
    val scrollIndicator: (@Composable androidx.compose.foundation.layout.BoxScope.() -> Unit)? =
        if (LocalScrollCaptureInProgress.current) null else {
            { ScrollIndicator(listState) }
        }

    ScreenScaffold(
        scrollState = listState,
        timeText = {},
        scrollIndicator = scrollIndicator,
    ) { contentPadding ->
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val topContentPadding = contentPadding.calculateTopPadding()
            val readingBlockMinHeight = (maxHeight - topContentPadding).coerceAtLeast(0.dp)
            val fontScale = LocalDensity.current.fontScale
            val circleDiameter = minOf(maxWidth, maxHeight)
            val headerMaxWidth = circleDiameter * 0.72f
            val headerText = stringResource(R.string.wear_your_heart_rate)
            val headerStyle = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                lineHeight = 13.sp,
                letterSpacing = 1.sp,
            )
            val headerLineCount = rememberTextMeasurer().measure(
                text = headerText,
                style = headerStyle,
                constraints = Constraints(
                    maxWidth = with(LocalDensity.current) { headerMaxWidth.roundToPx() },
                ),
            ).lineCount
            val statusCircleFraction = if (fontScale > 1f || headerLineCount > 1) 0.68f else 0.76f
            val statusMaxWidth = circleDiameter * statusCircleFraction
            val readingSpacing = if (fontScale > 1f) 0.dp else 4.dp
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "wear_reading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = readingBlockMinHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.offset(y = -(topContentPadding / 2)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(readingSpacing),
                        ) {
                            Text(
                                text = headerText,
                                modifier = Modifier.widthIn(max = headerMaxWidth),
                                style = headerStyle,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = presentation.heartRate?.toString() ?: "—",
                                style = MaterialTheme.typography.numeralExtraLarge.copy(
                                    lineHeight = 50.sp,
                                ),
                                color = when (presentation.readingStatus) {
                                    WearReadingStatus.ABOVE_LIMIT,
                                    WearReadingStatus.AT_LIMIT -> colors.error

                                    else -> colors.primary
                                },
                                maxLines = 1,
                            )
                            if (presentation.heartRate != null || presentation.limit != null) {
                                Text(
                                    text = if (presentation.limit == null) {
                                        stringResource(R.string.wear_bpm)
                                    } else {
                                        stringResource(R.string.wear_bpm_limit, presentation.limit)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp,
                                    ),
                                    color = colors.onBackground,
                                )
                            }
                            val statusLabel = when (presentation.readingStatus) {
                                WearReadingStatus.NO_READING -> noticeText(
                                    presentation.notice,
                                    state.status
                                )

                                WearReadingStatus.NO_LIMIT -> stringResource(R.string.wear_limit_not_set)
                                WearReadingStatus.WITHIN_LIMIT -> stringResource(R.string.wear_within_limit)
                                WearReadingStatus.AT_LIMIT -> stringResource(R.string.wear_at_limit)
                                WearReadingStatus.ABOVE_LIMIT -> stringResource(R.string.wear_above_limit)
                            }
                            Text(
                                text = if (presentation.groupCount > 0 &&
                                    presentation.readingStatus != WearReadingStatus.NO_READING
                                ) {
                                    stringResource(
                                        R.string.wear_status_with_group,
                                        statusLabel,
                                        presentation.groupCount
                                    )
                                } else {
                                    statusLabel
                                },
                                modifier = Modifier.widthIn(max = statusMaxWidth),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 12.sp,
                                ),
                                color = when (presentation.readingStatus) {
                                    WearReadingStatus.WITHIN_LIMIT -> colors.secondary
                                    WearReadingStatus.ABOVE_LIMIT,
                                    WearReadingStatus.AT_LIMIT -> colors.error

                                    else -> colors.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center,
                            )
                            val additionalNotice = presentation.notice?.takeIf {
                                presentation.heartRate != null && it != presentation.connectionNotice
                            }
                            additionalNotice?.let { notice ->
                                Text(
                                    text = noticeText(notice),
                                    style = MaterialTheme.typography.bodyExtraSmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                    ),
                                    color = if (notice in IMPORTANT_NOTICES) colors.error else colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            presentation.connectionNotice?.let { notice ->
                                Text(
                                    text = noticeText(notice),
                                    style = MaterialTheme.typography.bodyExtraSmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                    ),
                                    color = colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                item(key = "wear_workout_action") {
                    Button(
                        onClick = onToggleWorkout,
                        enabled = presentation.primaryAction != WearPrimaryAction.STARTING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec),
                        label = {
                            Text(
                                text = stringResource(
                                    when (presentation.primaryAction) {
                                        WearPrimaryAction.CONNECT -> R.string.connect_phone
                                        WearPrimaryAction.START -> R.string.start
                                        WearPrimaryAction.STOP -> R.string.stop
                                        WearPrimaryAction.STARTING -> R.string.start
                                    }
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }

                item(key = "wear_group_action") {
                    Button(
                        onClick = onOpenGroup,
                        enabled = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec),
                        label = {
                            Text(
                                text = stringResource(
                                    R.string.group_count,
                                    presentation.groupCount
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun WearGroupContent(
    state: WearWorkoutState,
    nowEpochMillis: Long,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    listState: TransformingLazyColumnState = rememberTransformingLazyColumnState(),
) {
    val transformationSpec = rememberTransformationSpec()
    val colors = MaterialTheme.colorScheme
    val groupPresentation = remember(state.group, nowEpochMillis) {
        state.group.toWearGroupPresentation(nowEpochMillis)
    }
    val rows = groupPresentation.members

    ScreenScaffold(
        scrollState = listState,
        timeText = {},
        scrollIndicator = if (LocalScrollCaptureInProgress.current) null else {
            { ScrollIndicator(listState) }
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = modifier,
        ) {
            item(key = "wear_group_close") {
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                    transformation = SurfaceTransformation(transformationSpec),
                    label = { Text(stringResource(R.string.close)) },
                )
            }
            item(key = "wear_group_summary") {
                Text(
                    text = stringResource(
                        R.string.wear_group_summary,
                        groupPresentation.shownCount,
                        groupPresentation.totalCount,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (rows.isEmpty()) {
                item(key = "wear_group_empty") {
                    Text(
                        text = stringResource(R.string.wear_group_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(items = rows, key = { it.userId }) { member ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = member.name.ifBlank { member.userId },
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onBackground,
                    )
                    Text(
                        text = when {
                            member.bpm != null && member.limit != null -> stringResource(
                                R.string.wear_member_bpm_limit,
                                member.bpm,
                                member.limit,
                            )

                            member.bpm != null -> stringResource(
                                R.string.wear_member_bpm,
                                member.bpm
                            )

                            member.limit != null -> stringResource(
                                R.string.wear_member_no_reading_limit,
                                member.limit,
                            )

                            else -> stringResource(R.string.wear_no_reading)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (member.isCritical) colors.error else colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WearAmbientContent(
    state: WearWorkoutState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = listState,
        timeText = {},
        scrollIndicator = null
    ) { contentPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = stringResource(
                    when {
                        state.isStarting -> R.string.status_starting
                        state.isRunning -> R.string.wear_workout_active
                        else -> R.string.status_ready
                    }
                ),
                modifier = Modifier.fillMaxWidth(0.72f),
                style = MaterialTheme.typography.labelMedium,
                color = AMBIENT_NEUTRAL,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    if (state.companionConnected) R.string.status_connected else R.string.status_phone_lost
                ),
                modifier = Modifier.fillMaxWidth(0.72f),
                style = MaterialTheme.typography.bodyExtraSmall,
                color = AMBIENT_NEUTRAL,
                textAlign = TextAlign.Center,
            )
            state.heartRateLimit?.let { limit ->
                Text(
                    text = stringResource(R.string.own_limit, limit),
                    modifier = Modifier.fillMaxWidth(0.72f),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = AMBIENT_NEUTRAL,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private val AMBIENT_NEUTRAL = Color(0xFFA8A8A8)

@Composable
private fun noticeText(notice: WearHomeNotice?, fallback: String): String =
    when (notice) {
        WearHomeNotice.READY -> stringResource(R.string.status_ready)
        WearHomeNotice.STARTING -> stringResource(R.string.status_starting)
        WearHomeNotice.HEART_RATE_UNAVAILABLE -> stringResource(R.string.status_hr_unavailable)
        WearHomeNotice.PERMISSIONS_REQUIRED -> stringResource(R.string.status_permissions)
        WearHomeNotice.WATCH_PERMISSION_REQUIRED -> stringResource(R.string.status_open_watch_permissions)
        WearHomeNotice.CONFLICTING_WORKOUT -> stringResource(R.string.status_conflicting_workout)
        WearHomeNotice.ALREADY_RUNNING -> stringResource(R.string.status_already_running)
        WearHomeNotice.START_FAILED -> stringResource(R.string.status_start_failed)
        WearHomeNotice.STOP_FAILED -> stringResource(R.string.status_stop_failed)
        WearHomeNotice.PHONE_CONNECT -> stringResource(R.string.status_phone_connect)
        WearHomeNotice.WATCH_NOT_SELECTED -> stringResource(R.string.status_connect_watch)
        WearHomeNotice.PLATFORM_UNAVAILABLE -> stringResource(R.string.status_platform_unavailable)
        WearHomeNotice.SELECT_PHONE -> stringResource(R.string.status_select_phone)
        WearHomeNotice.CONNECTED -> stringResource(R.string.status_connected)
        WearHomeNotice.PHONE_DISCONNECTED -> stringResource(R.string.status_phone_lost)
        WearHomeNotice.SESSION_SYNC_UNAVAILABLE -> stringResource(R.string.status_session_sync)
        WearHomeNotice.OTHER, null -> fallback
    }

@Composable
private fun noticeText(notice: WearHomeNotice): String =
    noticeText(notice = notice, fallback = "")

private val IMPORTANT_NOTICES = setOf(
    WearHomeNotice.PERMISSIONS_REQUIRED,
    WearHomeNotice.WATCH_PERMISSION_REQUIRED,
    WearHomeNotice.CONFLICTING_WORKOUT,
    WearHomeNotice.ALREADY_RUNNING,
    WearHomeNotice.START_FAILED,
    WearHomeNotice.STOP_FAILED,
    WearHomeNotice.PLATFORM_UNAVAILABLE,
    WearHomeNotice.SELECT_PHONE,
    WearHomeNotice.SESSION_SYNC_UNAVAILABLE,
)
