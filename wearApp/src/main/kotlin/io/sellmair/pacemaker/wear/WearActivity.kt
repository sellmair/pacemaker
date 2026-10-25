package io.sellmair.pacemaker.wear

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseType
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.rememberAmbientModeManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.sellmair.pacemaker.WATCH_PHONE_CAPABILITY
import io.sellmair.pacemaker.WatchCompanionDataLayer
import io.sellmair.pacemaker.WatchCompanionDeviceState
import io.sellmair.pacemaker.WatchCompanionMessage
import io.sellmair.pacemaker.WatchCompanionDiscoveryState
import io.sellmair.pacemaker.WatchCompanionNodeAvailable
import io.sellmair.pacemaker.resolvePinnedWatchPhoneNode
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WearActivity : ComponentActivity() {
    private enum class PermissionFlow { CONNECT, START }

    private var permissionFlow = PermissionFlow.CONNECT
    private var healthReady by mutableStateOf(false)
    private val companionScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + WearCompanionRuntime.events + WearCompanionRuntime.states
    )
    private val states = WearCompanionRuntime.states
    private var companionDataLayer: WatchCompanionDataLayer? = null
    private var helloInFlightNode: String? = null
    private var contentInstalled = false
    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) {
                if (permissionFlow == PermissionFlow.CONNECT) initializeCompanionTransport() else requestBackgroundPermissionOrStart()
            } else states.workout =
                states.workout.copy(status = "Allow heart rate access in Settings")
            render()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val identity = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
        states.workout = states.workout.copy(
            userId = identity.getString("userId", "") ?: "",
            userName = identity.getString("name", "Pacemaker") ?: "Pacemaker",
            heartRateLimit = identity.getInt("limit", 145), colorHue = identity.getFloat("hue", 0f),
            sourceSelected = identity.getBoolean("source-selected", false)
        )
        val future = HealthServices.getClient(this).exerciseClient.getCapabilitiesAsync()
        future.addListener({
            val caps = runCatching { future.get() }.getOrNull()
            healthReady =
                listOf(ExerciseType.RUNNING, ExerciseType.WALKING, ExerciseType.WORKOUT).any {
                    caps?.typeToCapabilities?.get(it)?.supportedDataTypes?.contains(DataType.HEART_RATE_BPM) == true
                }
            if (!healthReady) states.workout =
                states.workout.copy(status = "This watch cannot measure workout heart rate")
            render()
        }, ContextCompat.getMainExecutor(this))
        render()
        companionScope.launch(start = CoroutineStart.UNDISPATCHED) {
            WatchCompanionDiscoveryState.flow().collect { discovery ->
                onCompanionPresenceChanged(WatchCompanionDeviceState(
                    candidates = discovery.candidates,
                    platformAvailable = discovery.platformAvailable
                ))
            }
        }
        companionScope.collectEventsAsync<WatchCompanionNodeAvailable> { event ->
            if (event.nodeId == getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
                    .getString("phone-node-id", null)) sendHelloToSelectedPhone()
        }
        companionScope.launch { WearCompanionRuntime.expireGroupWhileActive() }
        initializeCompanionTransport()
    }

    private fun companionPermissions(includeHealth: Boolean): List<String> = buildList {
        if (includeHealth) {
            if (Build.VERSION.SDK_INT >= 36) add("android.permission.health.READ_HEART_RATE")
            else add(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun initializeCompanionTransport() {
        if (GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS
        ) {
            states.workout = states.workout.copy(
                companionConnected = false, status = "Google Play services is unavailable"
            )
            render()
            return
        }
        if (companionDataLayer == null) {
            companionDataLayer = WatchCompanionDataLayer(
                this, companionScope, WATCH_PHONE_CAPABILITY
            ).also {
                it.start()
            }
        } else companionScope.launch { companionDataLayer?.refreshPresence() }
    }

    private suspend fun onCompanionPresenceChanged(state: WatchCompanionDeviceState) {
        val prefs = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
        val pinned = prefs.getString("authenticated-phone-node-id", null)
        val target = resolvePinnedWatchPhoneNode(pinned, state.candidates)
        if (target != null) prefs.edit().putString("phone-node-id", target).apply()
        val connected = target != null
        val sourceSelected = prefs.getBoolean("source-selected", false)
        states.workout = states.workout.copy(
            companionConnected = connected,
            status = when {
                !state.platformAvailable -> "Google Play services is unavailable"
                !connected -> "Open Pacemaker on your phone to connect"
                states.workout.userId.isBlank() || !sourceSelected -> "Select this watch in Pacemaker"
                else -> "Connected to Pacemaker"
            }
        )
        render()
        if (connected) sendHelloToSelectedPhone()
    }

    private suspend fun sendHelloToSelectedPhone() {
        val prefs = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
        val nodeId = prefs.getString("phone-node-id", null) ?: return
        if (prefs.getString(
                "authenticated-phone-node-id",
                null
            ) == nodeId || helloInFlightNode == nodeId
        ) return
        val transport = companionDataLayer ?: return
        helloInFlightNode = nodeId
        val accepted = transport.sendReliableTo(
            nodeId,
            WatchCompanionMessage.Hello("wear-hello-${java.util.UUID.randomUUID()}"),
            retries = 2
        )
        if (helloInFlightNode == nodeId) helloInFlightNode = null
        if (prefs.getString("phone-node-id", null) != nodeId) return
        if (accepted) {
            prefs.edit().putString("authenticated-phone-node-id", nodeId).apply()
            states.workout =
                states.workout.copy(status = "Select this watch in Pacemaker")
        } else {
            states.workout = states.workout.copy(
                companionConnected = false, status = "Open Pacemaker on your phone to connect"
            )
        }
        render()
    }


    private fun toggleWorkout() {
        if (states.workout.isRunning) {
            startService(
                Intent(
                    this,
                    WearWorkoutService::class.java
                ).setAction(WearWorkoutService.ACTION_STOP)
            )
            return
        }
        if (!states.workout.companionConnected || !states.workout.sourceSelected || states.workout.userId.isBlank()) {
            initializeCompanionTransport()
            states.workout =
                states.workout.copy(status = "Open Pacemaker on your phone to connect")
            render()
            return
        }
        permissionFlow = PermissionFlow.START
        val missing = companionPermissions(includeHealth = true).filter {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) requestBackgroundPermissionOrStart() else permissionRequest.launch(
            missing.toTypedArray()
        )
    }

    private fun requestBackgroundPermissionOrStart() {
        val background = when {
            Build.VERSION.SDK_INT >= 36 -> "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
            Build.VERSION.SDK_INT in 33..35 -> Manifest.permission.BODY_SENSORS_BACKGROUND
            else -> null
        }
        if (background != null && ContextCompat.checkSelfPermission(
                this,
                background
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionFlow = PermissionFlow.START
            permissionRequest.launch(arrayOf(background))
        } else toggleWorkoutAfterPermissions()
    }

    private fun toggleWorkoutAfterPermissions() {
        val state = states.workout
        if (state.isRunning) startService(
            Intent(this, WearWorkoutService::class.java).setAction(
                WearWorkoutService.ACTION_STOP
            )
        )
        else if (!state.companionConnected || !state.sourceSelected || state.userId.isBlank()) states.workout =
            state.copy(status = "Open Pacemaker on your phone to connect")
        else if (!healthReady) states.workout =
            state.copy(status = "Heart rate sensor unavailable")
        else ContextCompat.startForegroundService(
            this,
            Intent(this, WearWorkoutService::class.java).setAction(WearWorkoutService.ACTION_START)
        )
        render()
    }

    override fun onDestroy() {
        companionDataLayer?.close()
        companionScope.cancel()
        super.onDestroy()
    }

    private fun render() {
        if (contentInstalled) return
        contentInstalled = true
        setContent {
            val ambientModeManager = rememberAmbientModeManager()
            CompositionLocalProvider(LocalAmbientModeManager provides ambientModeManager) {
                val ambient = ambientModeManager.currentAmbientMode is AmbientMode.Ambient
            val workout by states.getState(WearWorkoutState).collectAsStateWithLifecycle()
                val nowEpochMillis = rememberInteractiveWearNowEpochMillis(ambient)
                WearAppContent(
                    state = workout,
                    nowEpochMillis = nowEpochMillis,
                    ambient = ambient,
                    onToggleWorkout = ::toggleWorkout,
                )
            }
        }
    }
}
