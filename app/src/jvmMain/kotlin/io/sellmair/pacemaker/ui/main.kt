package io.sellmair.pacemaker.ui

import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import io.sellmair.pacemaker.JvmApplicationBackend
import io.sellmair.pacemaker.JvmHeartRateSensorBluetoothService
import io.sellmair.pacemaker.launchApplicationBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

 val appScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main +
        JvmApplicationBackend.events + JvmApplicationBackend.states
)

fun main() {

    JvmApplicationBackend.launchApplicationBackend(appScope)


    singleWindowApplication(
        title = "Pacemaker",
        alwaysOnTop = true,
        state = WindowState(position = WindowPosition.Aligned(Alignment.TopEnd), size = DpSize(400.dp, 800.dp)),
    ) {
        installEvas(appScope.coroutineContext.eventsOrThrow, appScope.coroutineContext.statesOrThrow) {
            ApplicationWindow()
        }
    }
}

