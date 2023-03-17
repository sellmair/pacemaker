@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.broadheart

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import io.sellmair.broadheart.service.MainService
import io.sellmair.broadheart.ui.ApplicationWindow
import io.sellmair.broadheart.viewModel.ApplicationViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val mainServiceConnection = MainServiceConnection()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coroutineContext = Dispatchers.Main + Job()

        requestPermissions(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ), 0
        )
        startForegroundService()

        setContent {
            val service by mainServiceConnection.service.collectAsState()
            service?.services?.let { services ->
                ApplicationWindow(
                    ApplicationViewModel(
                        this.lifecycleScope,
                        services.userService,
                        services.groupService
                    )
                )
            }
        }
    }

    private inner class MainServiceConnection : ServiceConnection {

        private val _service = MutableStateFlow<MainService.MainServiceBinder?>(null)

        val service = _service.asStateFlow()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MainService.MainServiceBinder) {
                this._service.value = service
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _service.value = null
        }
    }

    private fun startForegroundService() {
        bindService(Intent(this, MainService::class.java), mainServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mainServiceConnection)
        cancel()
    }
}
