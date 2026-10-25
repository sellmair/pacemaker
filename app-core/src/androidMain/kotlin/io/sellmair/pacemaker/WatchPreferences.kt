package io.sellmair.pacemaker

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

// Keep preference file names and persisted keys here so existing installations retain their data.
private const val APP_PREFERENCES = "app"
private const val PHONE_WATCH_PREFERENCES = "watch-companion"
private const val WEAR_COMPANION_PREFERENCES = "wear-companion"

private const val LAST_SELECTED_WATCH_NODE_ID = "last-selected-watch-node-id"
private const val WATCH_MEASUREMENT_RUN_ID = "watch-measurement-run-id"
private const val WATCH_MEASUREMENT_RUN_NODE_ID = "watch-measurement-run-node-id"
private const val WATCH_MEASUREMENT_STOPPED_RUN_IDS = "watch-measurement-stopped-run-ids"
private const val MEASUREMENT_STOP_QUEUE = "measurement-stop-queue-v1"
private const val MEASUREMENT_STOP_GENERATION = "measurement-stop-generation"

private const val PHONE_NODE_ID = "phone-node-id"
private const val AUTHENTICATED_PHONE_NODE_ID = "authenticated-phone-node-id"
private const val USER_ID = "userId"
private const val USER_NAME = "name"
private const val HEART_RATE_LIMIT = "limit"
private const val COLOR_HUE = "hue"
private const val SOURCE_SELECTED = "source-selected"
private const val ACTIVE_MEASUREMENT_RUN_ID = "active-measurement-run-id"

fun Context.appSettingsPreferences(): SharedPreferences =
    getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)

fun Context.phoneWatchPreferences(): SharedPreferences =
    getSharedPreferences(PHONE_WATCH_PREFERENCES, Context.MODE_PRIVATE)

fun Context.wearCompanionPreferences(): SharedPreferences =
    getSharedPreferences(WEAR_COMPANION_PREFERENCES, Context.MODE_PRIVATE)

var SharedPreferences.lastSelectedWatchNodeId: String?
    get() = getString(LAST_SELECTED_WATCH_NODE_ID, null)
    set(value) = edit { putString(LAST_SELECTED_WATCH_NODE_ID, value) }

val SharedPreferences.watchStoppedRunIds: Set<String>
    get() = getStringSet(WATCH_MEASUREMENT_STOPPED_RUN_IDS, emptySet()).orEmpty()

val SharedPreferences.watchActiveRunId: String?
    get() = getString(WATCH_MEASUREMENT_RUN_ID, null)

val SharedPreferences.watchActiveRunNodeId: String?
    get() = getString(WATCH_MEASUREMENT_RUN_NODE_ID, null)

private fun SharedPreferences.stoppedRunsIncluding(runId: String): Set<String> =
    (watchStoppedRunIds + runId).toList().takeLast(64).toSet()

fun SharedPreferences.markWatchRunStopped(runId: String, clearActive: Boolean) {
    val stopped = stoppedRunsIncluding(runId)
    edit {
        putStringSet(WATCH_MEASUREMENT_STOPPED_RUN_IDS, stopped)
        if (clearActive) {
            remove(WATCH_MEASUREMENT_RUN_ID)
            remove(WATCH_MEASUREMENT_RUN_NODE_ID)
        }
    }
}

fun SharedPreferences.activateWatchRun(runId: String, nodeId: String) {
    val previousRun = watchActiveRunId
    edit {
        if (previousRun != null && previousRun != runId) {
            putStringSet(WATCH_MEASUREMENT_STOPPED_RUN_IDS, stoppedRunsIncluding(previousRun))
        }
        putString(WATCH_MEASUREMENT_RUN_ID, runId)
        putString(WATCH_MEASUREMENT_RUN_NODE_ID, nodeId)
    }
}

var SharedPreferences.watchStopQueueJson: String?
    get() = getString(MEASUREMENT_STOP_QUEUE, null)
    set(value) = edit {
        if (value == null) remove(MEASUREMENT_STOP_QUEUE)
        else putString(MEASUREMENT_STOP_QUEUE, value)
    }

var SharedPreferences.watchStopGeneration: Long
    get() = getLong(MEASUREMENT_STOP_GENERATION, 0L)
    set(value) = edit { putLong(MEASUREMENT_STOP_GENERATION, value) }

var SharedPreferences.wearPhoneNodeId: String?
    get() = getString(PHONE_NODE_ID, null)
    set(value) = edit { putString(PHONE_NODE_ID, value) }

var SharedPreferences.wearAuthenticatedPhoneNodeId: String?
    get() = getString(AUTHENTICATED_PHONE_NODE_ID, null)
    set(value) = edit { putString(AUTHENTICATED_PHONE_NODE_ID, value) }

val SharedPreferences.wearUserId: String
    get() = getString(USER_ID, "") ?: ""

val SharedPreferences.wearUserName: String
    get() = getString(USER_NAME, "Pacemaker") ?: "Pacemaker"

val SharedPreferences.wearHeartRateLimit: Int
    get() = getInt(HEART_RATE_LIMIT, 145)

val SharedPreferences.wearColorHue: Float
    get() = getFloat(COLOR_HUE, 0f)

val SharedPreferences.wearSourceSelected: Boolean
    get() = getBoolean(SOURCE_SELECTED, false)

var SharedPreferences.wearActiveMeasurementRunId: String?
    get() = getString(ACTIVE_MEASUREMENT_RUN_ID, null)
    set(value) = edit {
        if (value == null) remove(ACTIVE_MEASUREMENT_RUN_ID)
        else putString(ACTIVE_MEASUREMENT_RUN_ID, value)
    }

val SharedPreferences.wearStopGeneration: Long
    get() = getLong(MEASUREMENT_STOP_GENERATION, 0L)

fun SharedPreferences.advanceWearStopGeneration(generation: Long) {
    if (generation > wearStopGeneration) edit { putLong(MEASUREMENT_STOP_GENERATION, generation) }
}

fun SharedPreferences.saveWearConfiguration(message: WatchCompanionMessage.Configure) {
    edit {
        putString(USER_ID, message.userId)
        putString(USER_NAME, message.userName)
        putInt(HEART_RATE_LIMIT, message.heartRateLimit)
        putFloat(COLOR_HUE, message.colorHue)
        putBoolean(SOURCE_SELECTED, message.sourceSelected)
    }
}
