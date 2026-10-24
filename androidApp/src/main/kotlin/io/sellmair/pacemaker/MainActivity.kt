@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import io.sellmair.evas.compose.LocalEvents
import io.sellmair.evas.compose.LocalStates
import io.sellmair.pacemaker.ui.ApplicationWindow
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import io.sellmair.pacemaker.ui.StartupScreen
import io.sellmair.pacemaker.ui.StartupStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()
    private var startupStatus by mutableStateOf(StartupStatus.Connecting)
    private var bluetoothEnableRequestInFlight = false
    private var bluetoothEnableRequestDismissed = false
    private var serviceBindRequested = false

    private val mainServiceConnection = ApplicationBackendConnection()
    private val runtimePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasBluetoothRuntimePermission()) {
            requestBluetoothEnableIfNeeded()
        } else {
            startupStatus = StartupStatus.PermissionRequired
            Toast.makeText(
                this,
                "Bluetooth permission is required to use Pacemaker.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private val bluetoothEnableRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        bluetoothEnableRequestInFlight = false
        val permissionsGranted = hasBluetoothRuntimePermission()
        val bluetoothEnabled = permissionsGranted &&
                getSystemService<BluetoothManager>()?.adapter?.isEnabled == true
        if (result.resultCode == Activity.RESULT_OK && bluetoothEnabled) {
            bluetoothEnableRequestDismissed = false
            startupStatus = StartupStatus.Connecting
            startForegroundService()
        } else {
            bluetoothEnableRequestDismissed = true
            startupStatus = if (permissionsGranted) {
                StartupStatus.Connecting
            } else {
                StartupStatus.PermissionRequired
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        coroutineContext = Dispatchers.Main + Job()

        val runtimePermissions = listOfNotNull(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
        )
        val permissionsToRequest = runtimePermissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isEmpty()) {
            requestBluetoothEnableIfNeeded()
        } else {
            startupStatus = StartupStatus.PermissionRequired
            runtimePermissionRequest.launch(permissionsToRequest.toTypedArray())
        }

        setContent {
            val backend = mainServiceConnection.backend.collectAsState().value
            if (backend != null) {
                CompositionLocalProvider(
                    LocalStates provides backend.states,
                    LocalEvents provides backend.events,
                    LocalSessionService provides backend.sessionService
                ) {
                    ApplicationWindow()
                }
            } else {
                SharedRhythmTheme {
                    StartupScreen(
                        status = startupStatus,
                        onRetryPermission = {
                            runtimePermissionRequest.launch(permissionsToRequest.toTypedArray())
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requestBluetoothEnableIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == AndroidHeartRateNotification.stopAction) {
            unbindService(mainServiceConnection)
            stopService(Intent(this, AndroidApplicationBackend::class.java))
            finish()
            Runtime.getRuntime().exit(0)
        }
    }

    private fun hasBluetoothRuntimePermission(): Boolean = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE
    ).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun requestBluetoothEnableIfNeeded() {
        if (!hasBluetoothRuntimePermission()) {
            startupStatus = StartupStatus.PermissionRequired
            return
        }

        // Keep the app usable when the user declines the separate Bluetooth-enable prompt.
        startupStatus = StartupStatus.Connecting
        startForegroundService()

        // BLUETOOTH_CONNECT is part of the complete grant check above, before adapter access.
        val adapter = getSystemService<BluetoothManager>()?.adapter
        if (adapter?.isEnabled == false) {
            startupStatus = StartupStatus.BluetoothRequired
            if (!bluetoothEnableRequestInFlight && !bluetoothEnableRequestDismissed) {
                bluetoothEnableRequestInFlight = true
                bluetoothEnableRequest.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
            return
        }

        bluetoothEnableRequestDismissed = false
    }

    private inner class ApplicationBackendConnection : ServiceConnection {

        private var coroutineContext: CoroutineContext? = null

        private val _backend = MutableStateFlow<ApplicationBackend?>(null)

        val backend = _backend.asStateFlow()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AndroidApplicationBackend.MainServiceBinder) {
                this._backend.value = service
                val coroutineContext =
                    SupervisorJob() + Dispatchers.Main + service.events + service.states
                CoroutineScope(coroutineContext).launchFrontendServices()
                this.coroutineContext = coroutineContext
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _backend.value = null
            serviceBindRequested = false
            startupStatus = StartupStatus.Connecting
            coroutineContext?.cancel()
        }
    }

    private fun startForegroundService() {
        if (serviceBindRequested) return
        serviceBindRequested = true
        bindService(
            Intent(this, AndroidApplicationBackend::class.java),
            mainServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
