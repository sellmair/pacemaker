package io.sellmair.pacemaker.ui.mainPage

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.sellmair.pacemaker.UtteranceState
import io.sellmair.pacemaker.ui.LocalStateBus
import io.sellmair.pacemaker.ui.MeColor
import io.sellmair.pacemaker.ui.MeColorLight
import io.sellmair.pacemaker.ui.collectAsState

@Composable
fun UtteranceControlButton(modifier: Modifier = Modifier) {
    val utteranceState by UtteranceState.collectAsState()
    val stateBus = LocalStateBus.current
    UtteranceControlButton(
        state = utteranceState,
        modifier = modifier,
        onClick = {
            stateBus?.setState(UtteranceState, utteranceState.next())
        }
    )
}

@Composable
fun UtteranceControlButton(
    state: UtteranceState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val desiredColor = when (state) {
        UtteranceState.Silence -> Color.Gray
        UtteranceState.Warnings -> MeColorLight()
        UtteranceState.All -> MeColor()
    }

    val color = remember { Animatable(desiredColor) }

    LaunchedEffect(desiredColor) {
        color.animateTo(desiredColor)
    }

    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = color.value
        ),
        onClick = onClick,
        modifier = modifier
            .padding(18.dp)
    ) {

        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn() + scaleIn())
                    .togetherWith(slideOutHorizontally { -it } + fadeOut() + scaleOut())
            },
        ) { targetState ->
            Icon(
                imageVector = when (targetState) {
                    UtteranceState.Silence -> Icons.Default.VolumeOff
                    UtteranceState.Warnings -> Icons.Default.Warning
                    UtteranceState.All -> Icons.Default.VolumeUp
                },
                contentDescription = null,
            )
        }
    }
}