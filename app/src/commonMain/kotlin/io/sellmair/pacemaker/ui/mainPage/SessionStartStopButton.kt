package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.sellmair.pacemaker.ActiveSessionIntent
import io.sellmair.pacemaker.ActiveSessionState
import io.sellmair.pacemaker.MeState
import io.sellmair.pacemaker.model.Session
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.ui.LocalEventBus
import io.sellmair.pacemaker.ui.collectAsState
import io.sellmair.pacemaker.ui.displayColor
import io.sellmair.pacemaker.ui.toColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toDateTimePeriod
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SessionStartStopButton(modifier: Modifier = Modifier) {
    val meState by MeState.collectAsState()
    val activeSessionState by ActiveSessionState.collectAsState()

    val eventBus = LocalEventBus.current
    val coroutineScope = rememberCoroutineScope()

    SessionStartStopButton(
        me = meState?.me ?: return,
        session = activeSessionState.session,
        onClick = {
            coroutineScope.launch {
                if (activeSessionState.session == null) eventBus?.emit(ActiveSessionIntent.Start)
                else eventBus?.emit(ActiveSessionIntent.Stop)
            }
        },
        modifier = modifier
    )
}

@Composable
fun SessionStartStopButton(
    me: User,
    session: Session?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = remember { Animatable(me.displayColor.toColor()) }

    LaunchedEffect(session) {
        color.animateTo(
            if (session != null) Color.Companion.Red else me.displayColor.toColor(),
        )
    }

    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = color.value
        ),
        onClick = onClick,
        modifier = modifier
            .padding(18.dp)
            .animateContentSize()
    ) {
        Icon(
            imageVector = if (session != null) Icons.Default.StopCircle else Icons.Default.RadioButtonChecked,
            contentDescription = "Start Session"
        )

        if (session != null) {
            SessionDurationText(session)
        }
    }
}

@Composable
fun SessionDurationText(
    session: Session,
    modifier: Modifier = Modifier
) {
    val timeSinceSessionStarted by flow {
        while (true) {
            delay(100.milliseconds)
            emit((Clock.System.now() - session.startTime))
        }
    }.collectAsState(0.milliseconds)

    val period = timeSinceSessionStarted.toDateTimePeriod()

    Spacer(Modifier.width(4.dp))

    Text(
        "${period.minutes}m ${period.seconds}s",
        fontSize = 8.sp,
        color = Color.White,
        modifier = modifier
    )
}