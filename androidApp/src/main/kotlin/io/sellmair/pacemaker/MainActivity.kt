@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker

import android.Manifest
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
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import io.sellmair.evas.compose.LocalEvents
import io.sellmair.evas.compose.LocalStates
import io.sellmair.pacemaker.ui.ApplicationWindow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val mainServiceConnection = ApplicationBackendConnection()
    private val runtimePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasBluetoothRuntimePermission()) {
            startForegroundService()
        } else {
            Toast.makeText(this, "Bluetooth permission is required to use Pacemaker.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.BLACK)
        )

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
            startForegroundService()
        } else {
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
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val bluetoothManager = getSystemService<BluetoothManager>()
        val adapter = bluetoothManager?.adapter

        lifecycleScope.launch {
            while (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                delay(1.seconds)
            }

            if (adapter?.isEnabled == false) {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                @Suppress("DEPRECATION")
                startActivityForResult(enableBluetoothIntent, -1)
            }
        }
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

    private inner class ApplicationBackendConnection : ServiceConnection {

        private var coroutineContext: CoroutineContext? = null

        private val _backend = MutableStateFlow<ApplicationBackend?>(null)

        val backend = _backend.asStateFlow()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is AndroidApplicationBackend.MainServiceBinder) {
                this._backend.value = service
                val coroutineContext = SupervisorJob() + Dispatchers.Main + service.events + service.states
                CoroutineScope(coroutineContext).launchFrontendServices()
                this.coroutineContext = coroutineContext
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _backend.value = null
            coroutineContext?.cancel()
        }
    }

    private fun startForegroundService() {
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
