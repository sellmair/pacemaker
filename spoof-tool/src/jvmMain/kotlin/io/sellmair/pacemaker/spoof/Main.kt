package io.sellmair.pacemaker.spoof

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import io.sellmair.evas.Events
import io.sellmair.evas.States
import io.sellmair.evas.compose.installEvas
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val appScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main + Events() + States(),
)

fun main() {
    appScope.launchSpoofToolActor()

    singleWindowApplication(
        title = "Pacemaker Spoof Tool",
        state = WindowState(
            position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center),
            size = DpSize(520.dp, 760.dp),
        ),
    ) {
        installEvas(appScope.coroutineContext.eventsOrThrow, appScope.coroutineContext.statesOrThrow) {
            SpoofToolWindow()
        }
    }
}
