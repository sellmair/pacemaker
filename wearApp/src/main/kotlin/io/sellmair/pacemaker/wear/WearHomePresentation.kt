package io.sellmair.pacemaker.wear

import io.sellmair.pacemaker.HeartRateSource

internal enum class WearReadingStatus {
    NO_READING,
    NO_LIMIT,
    WITHIN_LIMIT,
    AT_LIMIT,
    ABOVE_LIMIT,
}

internal enum class WearHomeNotice {
    READY,
    STARTING,
    HEART_RATE_UNAVAILABLE,
    PERMISSIONS_REQUIRED,
    WATCH_PERMISSION_REQUIRED,
    CONFLICTING_WORKOUT,
    ALREADY_RUNNING,
    START_FAILED,
    STOP_FAILED,
    PHONE_CONNECT,
    WATCH_NOT_SELECTED,
    PLATFORM_UNAVAILABLE,
    SELECT_PHONE,
    CONNECTED,
    PHONE_DISCONNECTED,
    SESSION_SYNC_UNAVAILABLE,
    OTHER,
}

internal enum class WearPrimaryAction {
    CONNECT,
    START,
    STOP,
    STARTING,
}

internal data class WearHomePresentation(
    val heartRate: Int?,
    val limit: Int?,
    val readingStatus: WearReadingStatus,
    val notice: WearHomeNotice?,
    val connectionNotice: WearHomeNotice?,
    val companionConnected: Boolean,
    val groupCount: Int,
    val primaryAction: WearPrimaryAction,
)

internal fun WearWorkoutState.toWearHomePresentation(nowEpochMillis: Long): WearHomePresentation {
    val measuredAt = measuredAtEpochMillis
    val isFresh = measuredAt != null &&
            (nowEpochMillis - measuredAt) in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds
    val currentHeartRate = heartRate.takeIf { isFresh }
    val readingStatus = when {
        currentHeartRate == null -> WearReadingStatus.NO_READING
        heartRateLimit == null -> WearReadingStatus.NO_LIMIT
        currentHeartRate > heartRateLimit -> WearReadingStatus.ABOVE_LIMIT
        currentHeartRate == heartRateLimit -> WearReadingStatus.AT_LIMIT
        else -> WearReadingStatus.WITHIN_LIMIT
    }
    val statusNotice = statusNotice(status, companionConnected)
    val notice = when {
        isStarting -> WearHomeNotice.STARTING
        statusNotice in IMPORTANT_NOTICES -> statusNotice
        readingStatus != WearReadingStatus.NO_READING -> null
        statusNotice == WearHomeNotice.PHONE_DISCONNECTED && !companionConnected -> statusNotice
        isRunning -> WearHomeNotice.HEART_RATE_UNAVAILABLE
        !companionConnected -> WearHomeNotice.PHONE_CONNECT
        !sourceSelected || userId.isBlank() -> WearHomeNotice.WATCH_NOT_SELECTED
        statusNotice == WearHomeNotice.READY || statusNotice == WearHomeNotice.CONNECTED -> statusNotice
        else -> WearHomeNotice.HEART_RATE_UNAVAILABLE
    }
    val connectionNotice = when {
        !companionConnected && currentHeartRate != null -> WearHomeNotice.PHONE_DISCONNECTED
        else -> null
    }
    val primaryAction = when {
        isRunning -> WearPrimaryAction.STOP
        isStarting -> WearPrimaryAction.STARTING
        !companionConnected || !sourceSelected || userId.isBlank() -> WearPrimaryAction.CONNECT
        else -> WearPrimaryAction.START
    }

    return WearHomePresentation(
        heartRate = currentHeartRate,
        limit = heartRateLimit,
        readingStatus = readingStatus,
        notice = notice,
        connectionNotice = connectionNotice,
        companionConnected = companionConnected,
        groupCount = group.toWearGroupPresentation(nowEpochMillis).totalCount,
        primaryAction = primaryAction,
    )
}

private fun statusNotice(status: String, companionConnected: Boolean): WearHomeNotice =
    when (status) {
        "Ready" -> WearHomeNotice.READY
        "Starting workout", "Checking heart rate" -> WearHomeNotice.STARTING
        "Heart rate unavailable", "Heart rate sensor unavailable",
        "This watch cannot measure workout heart rate" -> WearHomeNotice.HEART_RATE_UNAVAILABLE

        "Allow heart rate access in Settings" -> WearHomeNotice.PERMISSIONS_REQUIRED
        "Open watch to allow heart rate access" -> WearHomeNotice.WATCH_PERMISSION_REQUIRED
        "Stop the other workout first" -> WearHomeNotice.CONFLICTING_WORKOUT
        "Pacemaker workout already active" -> WearHomeNotice.ALREADY_RUNNING
        "Could not start workout", "Workout status unavailable" -> WearHomeNotice.START_FAILED
        "Could not stop workout" -> WearHomeNotice.STOP_FAILED
        "Open Pacemaker on your phone to connect" -> WearHomeNotice.PHONE_CONNECT
        "Select this watch in Pacemaker" -> WearHomeNotice.WATCH_NOT_SELECTED
        "Google Play services is unavailable" -> WearHomeNotice.PLATFORM_UNAVAILABLE
        "Open Pacemaker on your selected phone" -> WearHomeNotice.SELECT_PHONE
        "Connected to Pacemaker" -> WearHomeNotice.CONNECTED
        "Phone connection lost" -> WearHomeNotice.PHONE_DISCONNECTED
        "With phone" -> if (companionConnected) WearHomeNotice.CONNECTED else WearHomeNotice.PHONE_DISCONNECTED
        "Session sync unavailable" -> WearHomeNotice.SESSION_SYNC_UNAVAILABLE
        else -> WearHomeNotice.OTHER
    }

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
