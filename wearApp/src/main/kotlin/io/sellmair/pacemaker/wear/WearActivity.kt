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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseType
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import io.sellmair.pacemaker.WATCH_PHONE_CAPABILITY
import io.sellmair.pacemaker.WatchCompanionDataLayer
import io.sellmair.pacemaker.WatchCompanionDeviceState
import io.sellmair.pacemaker.WatchCompanionMessage
import io.sellmair.pacemaker.WatchCompanionCodec
import io.sellmair.pacemaker.resolvePinnedWatchPhoneNode
import io.sellmair.pacemaker.awaitPacemaker
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WearActivity : ComponentActivity() {
    private enum class PermissionFlow { CONNECT, START }
    private var permissionFlow = PermissionFlow.CONNECT
    private var healthReady by mutableStateOf(false)
    private val companionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var companionDataLayer: WatchCompanionDataLayer? = null
    private var helloInFlightNode: String? = null
    private var contentInstalled = false
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.all { it }) {
            if (permissionFlow == PermissionFlow.CONNECT) initializeCompanionTransport() else requestBackgroundPermissionOrStart()
        } else WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(status = "Allow heart rate access in Settings")
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Remove legacy standalone selection and ownership settings while retaining paired identity.
        getSharedPreferences("wear", Context.MODE_PRIVATE).edit().remove("mode").apply()
        getSharedPreferences("wear-companion", Context.MODE_PRIVATE).edit()
            .remove("watch-only-authorized").remove("watch-owns-participant").remove("ownership-pending").apply()
        val identity = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
        WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
            userId = identity.getString("userId", "") ?: "",
            userName = identity.getString("name", "Pacemaker") ?: "Pacemaker",
            heartRateLimit = identity.getInt("limit", 145), colorHue = identity.getFloat("hue", 0f),
            sourceSelected = identity.getBoolean("source-selected", false)
        )
        val future = HealthServices.getClient(this).exerciseClient.getCapabilitiesAsync()
        future.addListener({
            val caps = runCatching { future.get() }.getOrNull()
            healthReady = listOf(ExerciseType.RUNNING, ExerciseType.WALKING, ExerciseType.WORKOUT).any {
                caps?.typeToCapabilities?.get(it)?.supportedDataTypes?.contains(DataType.HEART_RATE_BPM) == true
            }
            if (!healthReady) WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(status = "This watch cannot measure workout heart rate")
            render()
        }, ContextCompat.getMainExecutor(this))
        render()
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
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                companionConnected = false, status = "Google Play services is unavailable"
            )
            render()
            return
        }
        if (companionDataLayer == null) {
            companionDataLayer = WatchCompanionDataLayer(
                this, companionScope, WATCH_PHONE_CAPABILITY,
                selectedNodeId = { getSharedPreferences("wear-companion", Context.MODE_PRIVATE).getString("phone-node-id", null) },
                onPresenceChanged = ::onCompanionPresenceChanged,
                onNodeAvailable = {
                    sendHelloToSelectedPhone()
                }
            ).also {
                WearCompanionRuntime.addTransport(it)
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
        WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
            companionConnected = connected,
            status = when {
                !state.platformAvailable -> "Google Play services is unavailable"
                !connected -> "Open Pacemaker on your phone to connect"
                WearWorkoutUiState.state.value.userId.isBlank() || !sourceSelected -> "Select this watch in Pacemaker"
                else -> "Connected to Pacemaker"
            }
        )
        render()
        if (connected) sendHelloToSelectedPhone()
    }

    private suspend fun sendHelloToSelectedPhone() {
        val prefs = getSharedPreferences("wear-companion", Context.MODE_PRIVATE)
        val nodeId = prefs.getString("phone-node-id", null) ?: return
        if (prefs.getString("authenticated-phone-node-id", null) == nodeId || helloInFlightNode == nodeId) return
        val transport = companionDataLayer ?: return
        helloInFlightNode = nodeId
        val accepted = transport.sendReliable(WatchCompanionMessage.Hello("wear-hello-${java.util.UUID.randomUUID()}"), retries = 2)
        if (helloInFlightNode == nodeId) helloInFlightNode = null
        if (accepted) {
            prefs.edit().putString("authenticated-phone-node-id", nodeId).apply()
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(status = "Select this watch in Pacemaker")
        } else {
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(
                companionConnected = false, status = "Open Pacemaker on your phone to connect"
            )
        }
        render()
    }

    companion object {
    }

    private fun toggleWorkout() {
        if (WearWorkoutUiState.state.value.isRunning) {
            startService(Intent(this, WearWorkoutService::class.java).setAction(WearWorkoutService.ACTION_STOP))
            return
        }
        if (!WearWorkoutUiState.state.value.companionConnected || !WearWorkoutUiState.state.value.sourceSelected || WearWorkoutUiState.state.value.userId.isBlank()) {
            initializeCompanionTransport()
            WearWorkoutUiState.state.value = WearWorkoutUiState.state.value.copy(status = "Open Pacemaker on your phone to connect")
            render()
            return
        }
        permissionFlow = PermissionFlow.START
        val missing = companionPermissions(includeHealth = true).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) requestBackgroundPermissionOrStart() else permissionRequest.launch(missing.toTypedArray())
    }

    private fun requestBackgroundPermissionOrStart() {
        val background = when {
            Build.VERSION.SDK_INT >= 36 -> "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
            Build.VERSION.SDK_INT in 33..35 -> Manifest.permission.BODY_SENSORS_BACKGROUND
            else -> null
        }
        if (background != null && ContextCompat.checkSelfPermission(this, background) != PackageManager.PERMISSION_GRANTED) {
            permissionFlow = PermissionFlow.START
            permissionRequest.launch(arrayOf(background))
        } else toggleWorkoutAfterPermissions()
    }

    private fun toggleWorkoutAfterPermissions() {
        val state = WearWorkoutUiState.state.value
        if (state.isRunning) startService(Intent(this, WearWorkoutService::class.java).setAction(WearWorkoutService.ACTION_STOP))
        else if (!state.companionConnected || !state.sourceSelected || state.userId.isBlank()) WearWorkoutUiState.state.value =
            state.copy(status = "Open Pacemaker on your phone to connect")
        else if (!healthReady) WearWorkoutUiState.state.value = state.copy(status = "Heart rate sensor unavailable")
        else ContextCompat.startForegroundService(this, Intent(this, WearWorkoutService::class.java).setAction(WearWorkoutService.ACTION_START))
        render()
    }

    override fun onDestroy() {
        companionDataLayer?.let(WearCompanionRuntime::removeTransport)
        companionDataLayer?.close()
        companionScope.cancel()
        super.onDestroy()
    }

    private fun render() {
        if (contentInstalled) return
        contentInstalled = true
        setContent {
            MaterialTheme {
                val workout by WearWorkoutUiState.state.collectAsState()
                AppScaffold(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    WearHome(workout, ::toggleWorkout)
                }
            }
        }
    }
}

@Composable
private fun WearHome(
    workout: WearWorkoutState,
    onToggle: () -> Unit
) {
    var showGroupPage by androidx.compose.runtime.remember { mutableStateOf(false) }
    val status = when (workout.status) {
        "Ready" -> androidx.compose.ui.res.stringResource(R.string.status_ready)
        "Starting workout", "Checking heart rate" -> androidx.compose.ui.res.stringResource(R.string.status_starting)
        "Heart rate unavailable", "Heart rate sensor unavailable", "This watch cannot measure workout heart rate" -> androidx.compose.ui.res.stringResource(R.string.status_hr_unavailable)
        "Allow heart rate access in Settings" -> androidx.compose.ui.res.stringResource(R.string.status_permissions)
        "Open watch to allow heart rate access" -> androidx.compose.ui.res.stringResource(R.string.status_open_watch_permissions)
        "Phone connection lost" -> androidx.compose.ui.res.stringResource(R.string.status_phone_lost)
        "With phone" -> androidx.compose.ui.res.stringResource(R.string.status_with_phone)
        "Session sync unavailable" -> androidx.compose.ui.res.stringResource(R.string.status_session_sync)
        "Stop the other workout first" -> androidx.compose.ui.res.stringResource(R.string.status_conflicting_workout)
        "Pacemaker workout already active" -> androidx.compose.ui.res.stringResource(R.string.status_already_running)
        "Could not start workout", "Workout status unavailable" -> androidx.compose.ui.res.stringResource(R.string.status_start_failed)
        "Could not stop workout" -> androidx.compose.ui.res.stringResource(R.string.status_stop_failed)
        "Open Pacemaker on your phone to connect" -> androidx.compose.ui.res.stringResource(R.string.status_phone_connect)
        "Select this watch in Pacemaker" -> androidx.compose.ui.res.stringResource(R.string.status_connect_watch)
        "Google Play services is unavailable" -> androidx.compose.ui.res.stringResource(R.string.status_platform_unavailable)
        "Connected to Pacemaker" -> androidx.compose.ui.res.stringResource(R.string.status_connected)
        else -> workout.status
    }
    if (!showGroupPage) ScreenScaffold { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            // A centered safe column fits within the circular viewport at larger font scales.
            val heartRateSize = if (maxHeight < 190.dp) 34.sp else 38.sp
            Column(Modifier.fillMaxWidth(.68f).verticalScroll(rememberScrollState())
                .padding(top = 28.dp, bottom = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(workout.userName, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(androidx.compose.ui.res.stringResource(R.string.group_count, workout.group.size),
                    Modifier.clickable(enabled = workout.group.isNotEmpty()) { showGroupPage = true }.padding(vertical = 2.dp),
                    fontSize = 10.sp, maxLines = 1, color = if (workout.group.isNotEmpty()) Color.White else Color.DarkGray)
                Text(workout.heartRate?.toString() ?: "—", fontSize = heartRateSize, maxLines = 1)
                Text(if (workout.heartRate == null) status else "BPM · $status", fontSize = 10.sp,
                    textAlign = TextAlign.Center)
                workout.heartRateLimit?.let { Text(androidx.compose.ui.res.stringResource(R.string.own_limit, it), fontSize = 10.sp) }
                Button(onClick = onToggle, enabled = !workout.isStarting,
                    modifier = Modifier.fillMaxWidth(.92f), label = {
                        Text(androidx.compose.ui.res.stringResource(when {
                            workout.isRunning -> R.string.stop
                            !workout.companionConnected || !workout.sourceSelected || workout.userId.isBlank() -> R.string.connect_phone
                            else -> R.string.start
                        }))
                    })
            }
        }
    } else {
        val state = rememberTransformingLazyColumnState()
        val spec = rememberTransformationSpec()
        val rows = workout.group.sortedByDescending { it.isCritical }.take(12)
        ScreenScaffold(scrollState = state) { padding ->
            TransformingLazyColumn(state = state, contentPadding = padding, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                item { Button(onClick = { showGroupPage = false }, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec), label = { Text(androidx.compose.ui.res.stringResource(R.string.close)) }) }
                items(rows) { member -> Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(member.name, Modifier.weight(1f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(member.bpm?.toString() ?: "—", fontSize = 11.sp, color = if (member.isCritical) Color.Red else Color.White)
                } }
            }
        }
    }
}
