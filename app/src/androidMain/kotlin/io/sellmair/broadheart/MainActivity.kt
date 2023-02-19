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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import io.sellmair.broadheart.hrSensor.HeartRate
import io.sellmair.broadheart.service.GroupService
import io.sellmair.broadheart.service.MainService
import io.sellmair.broadheart.service.UserService
import io.sellmair.broadheart.ui.mainPage.MainPage
import io.sellmair.broadheart.ui.Route
import io.sellmair.broadheart.ui.settingsPage.SettingsPage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlin.coroutines.CoroutineContext


class MainActivity : ComponentActivity(), CoroutineScope {

    override var coroutineContext: CoroutineContext = Dispatchers.Main + Job()

    private val mainServiceConnection = MainServiceConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        coroutineContext = Dispatchers.Main + Job()

        requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), 0)
        startForegroundService()

        val groupStates = mainServiceConnection.groupService
            .flatMapLatest { servicesOrNull -> servicesOrNull?.groupState ?: flowOf(null) }

        setContent {
            var route by remember { mutableStateOf(Route.MainPage) }
            val groupState by groupStates.collectAsState(null)

            when (route) {
                Route.MainPage -> MainPage(
                    groupState = groupState,
                    onRoute = { route = it },
                    onMyHeartRateLimitChanged = { newHeartRateLimit ->
                        mainServiceConnection.updateHeartRateLimitChannel.trySend(newHeartRateLimit)
                    }
                )

                Route.SettingsPage -> {
                    SettingsPage(
                        userService = mainServiceConnection.userService.value ?: return@setContent,
                        groupService = mainServiceConnection.groupService.value ?: return@setContent,
                        onBack = { route = Route.MainPage }
                    )
                }
            }
        }
    }

    private inner class MainServiceConnection : ServiceConnection {
        private val _userService = MutableStateFlow<UserService?>(null)
        private val _groupService = MutableStateFlow<GroupService?>(null)

        val userService = _userService.asStateFlow()
        val groupService = _groupService.asStateFlow()

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MainService.MainServiceBinder) {
                _userService.tryEmit(service.services.userService)
                _groupService.tryEmit(service.services.groupService)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _userService.tryEmit(null)
            _groupService.tryEmit(null)
        }

        val updateHeartRateLimitChannel = Channel<HeartRate>(Channel.CONFLATED)

        init {
            launch {
                updateHeartRateLimitChannel.consumeEach { newHeartRateLimit ->
                    val userService = userService.filterNotNull().first()
                    val groupService = groupService.filterNotNull().first()
                    val me = userService.currentUser()
                    userService.saveUpperHeartRateLimit(me, newHeartRateLimit)
                    groupService.updateState()
                }
            }
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
