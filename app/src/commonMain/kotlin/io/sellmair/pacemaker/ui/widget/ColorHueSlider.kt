package io.sellmair.pacemaker.ui.widget

import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.sellmair.pacemaker.MeColorIntent
import io.sellmair.pacemaker.MeColorState
import io.sellmair.pacemaker.UserColors
import io.sellmair.pacemaker.ui.collectAsState
import io.sellmair.pacemaker.ui.toColor
import io.sellmair.pacemaker.utils.emit

@Composable
fun ColorHueSlider(
    modifier: Modifier = Modifier
) {
    val meColor = MeColorState.collectAsState().value?.color ?: return

    Slider(
        valueRange = 0f..360f,
        value = meColor.hue,
        onValueChange = Launching { newHue ->
            MeColorIntent.ChangeHue(newHue).emit()
        },
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = meColor.toColor(),
            activeTrackColor = UserColors.fromHueLight(meColor.hue).toColor(),
            inactiveTrackColor = meColor.toColor()
        )
    )
}