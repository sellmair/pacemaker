package io.sellmair.pacemaker.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.SampleDataPoint
import io.sellmair.evas.flow
import io.sellmair.pacemaker.WatchCompanionDiscoveryState
import io.sellmair.pacemaker.HeartRateSource
import io.sellmair.pacemaker.WatchCompanionMessage
import io.sellmair.pacemaker.WatchCompanionDataLayer
import io.sellmair.pacemaker.WATCH_PHONE_CAPABILITY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineStart
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

class WearWorkoutService : Service(), CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main.immediate + SupervisorJob() +
        WearCompanionRuntime.events + WearCompanionRuntime.states
    private val states = WearCompanionRuntime.states
    private val exerciseClient: ExerciseClient by lazy { HealthServices.getClient(this).exerciseClient }
    private var companionClient: WatchCompanionDataLayer? = null
    private var runId = ""
    private var heartRateExpiryJob: Job? = null
    private var startingWorkout = false
    private var exerciseStartInFlight = false
    private var workoutGeneration = 0L
    private var registeredUpdateCallback: ExerciseUpdateCallback? = null

    private fun updateCallbackFor(generation: Long) = object : ExerciseUpdateCallback {
        override fun onRegistered() = Unit
        override fun onRegistrationFailed(throwable: Throwable) {
            if (generation != workoutGeneration) return
            states.workout = states.workout.copy(
                isRunning = false, heartRate = null, status = "Heart rate sensor unavailable"
            )
            startingWorkout = false
            stopForeground(STOP_FOREGROUND_REMOVE)
        }

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            if (generation != workoutGeneration) return
            val point = update.latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()
            val bpm = point?.value?.toInt()
            val sampleTime = (point as? SampleDataPoint<*>)?.timeDurationFromBoot?.toMillis()
            if (bpm != null && bpm in 25..240 && sampleTime != null) {
                val measuredAt =
                    System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime() + sampleTime
                onHeartRate(bpm, measuredAt, generation)
            }
            if (update.exerciseStateInfo.state.isEnded) {
                stopWorkout(endExercise = false)
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) =
            Unit
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        launch(start = CoroutineStart.UNDISPATCHED) {
            WatchCompanionDiscoveryState.flow().collect { discovery ->
                val selected = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                    .getString("phone-node-id", null)
                val connected = discovery.candidates.any { it.id == selected && it.connected }
                states.workout = states.workout.copy(
                    companionConnected = connected,
                    status = if (connected) "With phone" else "Phone connection lost"
                )
            }
        }
        launch { WearCompanionRuntime.expireGroupWhileActive() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MEASUREMENT_STOP -> {
                val nodeId = intent.getStringExtra(EXTRA_NODE_ID)
                val messageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
                val stopGeneration = intent.getLongExtra(EXTRA_GENERATION, 0L)
                val targetRunId = intent.getStringExtra(EXTRA_RUN_ID)
                if (nodeId != null && messageId != null) {
                    val prefs = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                    val previousStop = prefs.getLong("measurement-stop-generation", 0L)
                    val activeRun = prefs.getString("active-measurement-run-id", null)
                    if (!io.sellmair.pacemaker.shouldStopWatchMeasurement(
                            activeRun, previousStop, targetRunId, stopGeneration
                        )) {
                        if (stopGeneration > previousStop) prefs.edit()
                            .putLong("measurement-stop-generation", stopGeneration).apply()
                        val anotherRunIsActive =
                            activeServiceExercise || startingWorkout || exerciseStartInFlight
                        startPhoneTransport()
                        launch {
                            companionClient?.sendTo(
                                nodeId,
                                WatchCompanionMessage.Ack(messageId, true, targetRunId)
                            )
                            if (!anotherRunIsActive) stopSelf()
                        }
                        return START_NOT_STICKY
                    }
                    val type =
                        if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification("Stopping heart rate"),
                        type
                    )
                    startPhoneTransport()
                    val stoppedRunId = targetRunId ?: activeRun
                    stopWorkout(sendToPhone = false) { stopped ->
                        launch {
                            if (stopped) prefs.edit()
                                .putLong("measurement-stop-generation", stopGeneration).apply()
                            companionClient?.sendTo(
                                nodeId, WatchCompanionMessage.Ack(
                                messageId, stopped, stoppedRunId.takeIf { stopped }
                            ))
                            if (stopped) stopSelf()
                        }
                    }
                }
            }

            ACTION_START -> startWorkout()

            ACTION_STOP -> stopWorkout()
        }
        return START_STICKY
    }

    @Suppress("RestrictedApi") // Health Services exposes this ownership status for safe workout adoption.
    private fun startWorkout() {
        if (states.workout.isRunning || startingWorkout || exerciseStartInFlight) return
        if (!states.workout.companionConnected || states.workout.userId.isBlank()) {
            states.workout =
                states.workout.copy(status = "Open Pacemaker on your phone to connect")
            return
        }
        val generation = ++workoutGeneration
        runId = java.util.UUID.randomUUID().toString()
        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
            .putString("active-measurement-run-id", runId).apply()
        activeServiceExercise = true
        startingWorkout = true
        states.workout = states.workout.copy(
            status = "Checking heart rate", isStarting = true
        )
        val foregroundType =
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification("Starting Pacemaker"),
            foregroundType
        )

        val capabilitiesFuture = exerciseClient.getCapabilitiesAsync()
        val exerciseInfoFuture = exerciseClient.getCurrentExerciseInfoAsync()
        exerciseInfoFuture.addListener({
            if (generation != workoutGeneration) return@addListener
            val info = runCatching { exerciseInfoFuture.get() }.getOrNull()
            if (info == null) {
                startingWorkout = false
                activeServiceExercise = false
                states.workout = states.workout.copy(
                    isStarting = false, isRunning = false, status = "Workout status unavailable"
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                return@addListener
            }
            if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS) {
                startingWorkout = false
                activeServiceExercise = false
                states.workout = states.workout.copy(
                    status = "Stop the other workout first", isRunning = false, isStarting = false
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                return@addListener
            }
            if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
                startingWorkout = false
                states.workout = states.workout.copy(
                    status = "Pacemaker workout already active",
                    isRunning = true,
                    isStarting = false
                )
                val callback = updateCallbackFor(generation)
                registeredUpdateCallback = callback
                exerciseClient.setUpdateCallback(callback)
                startPhoneTransport()
                return@addListener
            }
            capabilitiesFuture.addListener({
                if (generation != workoutGeneration) return@addListener
                val capabilities = runCatching { capabilitiesFuture.get() }.getOrNull()
                val workoutType =
                    listOf(ExerciseType.RUNNING, ExerciseType.WALKING, ExerciseType.WORKOUT)
                        .firstOrNull { type ->
                            capabilities?.typeToCapabilities?.get(type)?.supportedDataTypes?.contains(
                                DataType.HEART_RATE_BPM
                            ) == true
                        }
                if (workoutType == null) {
                    startingWorkout = false
                    activeServiceExercise = false
                    getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                        .remove("active-measurement-run-id").apply()
                    states.workout = states.workout.copy(
                        isRunning = false,
                        isStarting = false,
                        status = "Heart rate sensor unavailable"
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    return@addListener
                }
                val callback = updateCallbackFor(generation)
                registeredUpdateCallback = callback
                exerciseClient.setUpdateCallback(callback)
                val config = ExerciseConfig.builder(workoutType)
                    .setDataTypes(setOf(DataType.HEART_RATE_BPM))
                    .setIsAutoPauseAndResumeEnabled(false)
                    .setIsGpsEnabled(false)
                    .build()
                val startFuture = exerciseClient.startExerciseAsync(config)
                exerciseStartInFlight = true
                startFuture.addListener({
                    exerciseStartInFlight = false
                    if (generation != workoutGeneration) {
                        if (runCatching { startFuture.get() }.isSuccess) exerciseClient.endExerciseAsync()
                        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                            .remove("active-measurement-run-id").apply()
                        return@addListener
                    }
                    if (runCatching { startFuture.get() }.isFailure) {
                        startingWorkout = false
                        activeServiceExercise = false
                        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                            .remove("active-measurement-run-id").apply()
                        states.workout = states.workout.copy(
                            isRunning = false,
                            isStarting = false,
                            status = "Could not start workout"
                        )
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        return@addListener
                    }
                    startingWorkout = false
                    states.workout = states.workout.copy(
                        isRunning = true, isStarting = false, status = "With phone"
                    )
                    startPhoneTransport()
                }, ContextCompat.getMainExecutor(this))
            }, ContextCompat.getMainExecutor(this))
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startPhoneTransport() {
        if (companionClient == null) {
            companionClient = WatchCompanionDataLayer(
                this, this, WATCH_PHONE_CAPABILITY
            ).also { it.start() }
        }
        launch { companionClient?.refreshPresence() }
    }

    private fun onHeartRate(bpm: Int, measuredAt: Long, generation: Long) {
        if (generation != workoutGeneration || (!startingWorkout && !states.workout.isRunning)) return
        val age = System.currentTimeMillis() - measuredAt
        if (age !in 0..HeartRateSource.MAX_AGE.inWholeMilliseconds) return
        heartRateExpiryJob?.cancel()
        heartRateExpiryJob = launch {
            delay((HeartRateSource.MAX_AGE.inWholeMilliseconds - age).coerceAtLeast(0))
            val current = states.workout
            if (current.measuredAtEpochMillis == measuredAt) {
                states.workout =
                    current.copy(heartRate = null, status = "Heart rate unavailable")
            }
        }
        states.workout = states.workout.copy(
            isRunning = true, heartRate = bpm,
            measuredAtEpochMillis = measuredAt,
            status = "With phone"
        )
        val identity = states.workout
        if (identity.sourceSelected && identity.userId.isNotBlank()) launch {
            sendToPhone(
                WatchCompanionMessage.HeartRate(
                    messageId = "hr-${java.util.UUID.randomUUID()}",
                    userId = identity.userId,
                    bpm = bpm,
                    measuredAtEpochMillis = measuredAt,
                    measurementRunId = runId
                )
            )
        }
        alertIfCritical("me", bpm, identity.heartRateLimit)
    }

    private suspend fun sendToPhone(message: WatchCompanionMessage) {
        val nodeId = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
            .getString("phone-node-id", null) ?: return
        companionClient?.sendTo(nodeId, message)
    }

    private fun alertIfCritical(userId: String, bpm: Int, limit: Int?) {
        WearCompanionRuntime.alertIfCritical(this, userId, bpm, limit)
    }

    private fun stopHeartRateTransport() {
        companionClient?.close()
        companionClient = null
    }

    @Suppress("RestrictedApi") // Health Services exposes this ownership status for safely ending an orphan workout.
    private fun stopWorkout(
        sendToPhone: Boolean = true,
        endExercise: Boolean = true,
        completion: ((Boolean) -> Unit)? = null
    ) {
        if (sendToPhone) {
            val userId = states.workout.userId
            if (userId.isNotBlank()) launch {
                sendToPhone(
                    WatchCompanionMessage.HeartRate(
                        messageId = "hr-clear-${java.util.UUID.randomUUID()}",
                        userId = userId,
                        bpm = null,
                        measuredAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                        measurementRunId = runId
                    )
                )
            }
        }
        val wasRunning = states.workout.isRunning
        val endingFuture =
            if (endExercise && wasRunning) exerciseClient.endExerciseAsync() else null
        val shouldCheckOrphan = endExercise && !wasRunning && completion != null
        workoutGeneration++
        startingWorkout = false
        registeredUpdateCallback?.let { runCatching { exerciseClient.clearUpdateCallbackAsync(it) } }
        registeredUpdateCallback = null
        heartRateExpiryJob?.cancel()
        states.workout = states.workout.copy(
            isRunning = false,
            isStarting = false,
            heartRate = null,
            status = "Ready"
        )
        fun finishStop(ended: Boolean) {
            activeServiceExercise = !ended
            if (ended) {
                getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
                    .remove("active-measurement-run-id").apply()
                runId = ""
                stopForeground(STOP_FOREGROUND_REMOVE)
                completion?.invoke(true)
                if (completion == null) stopSelf()
                return
            }
            states.workout = states.workout.copy(
                isRunning = true, isStarting = false, status = "Could not stop workout"
            )
            val callback = updateCallbackFor(workoutGeneration)
            registeredUpdateCallback = callback
            exerciseClient.setUpdateCallback(callback)
            completion?.invoke(false)
        }
        if (endingFuture != null) {
            endingFuture.addListener(
                { finishStop(runCatching { endingFuture.get() }.isSuccess) },
                ContextCompat.getMainExecutor(this)
            )
        } else if (shouldCheckOrphan) {
            val infoFuture = exerciseClient.getCurrentExerciseInfoAsync()
            infoFuture.addListener({
                val info = runCatching { infoFuture.get() }.getOrNull()
                if (info == null) finishStop(false)
                else if (info.exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
                    val orphanStop = exerciseClient.endExerciseAsync()
                    orphanStop.addListener(
                        { finishStop(runCatching { orphanStop.get() }.isSuccess) },
                        ContextCompat.getMainExecutor(this)
                    )
                } else finishStop(true)
            }, ContextCompat.getMainExecutor(this))
        } else finishStop(true)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pacemaker workout",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.pacemaker_launcher_monochrome)
            .setContentTitle("Pacemaker")
            .setContentText(text)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        stopHeartRateTransport()
        cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var activeServiceExercise = false
        fun isWorkoutActiveOrEnding(): Boolean = activeServiceExercise

        const val ACTION_START = "io.sellmair.pacemaker.wear.START"
        const val ACTION_STOP = "io.sellmair.pacemaker.wear.STOP"
        const val ACTION_MEASUREMENT_STOP = "io.sellmair.pacemaker.wear.MEASUREMENT_STOP"
        const val EXTRA_NODE_ID = "nodeId"
        const val EXTRA_MESSAGE_ID = "messageId"
        const val EXTRA_GENERATION = "measurementStopGeneration"
        const val EXTRA_RUN_ID = "runId"
        private const val NOTIFICATION_ID = 50
        private const val CHANNEL_ID = "pacemaker_watch_workout"
    }
}
