package io.sellmair.pacemaker.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import io.sellmair.pacemaker.ui.MeColor
import io.sellmair.pacemaker.ui.MeColorLight

@Composable
internal fun GradientBackdrop(modifier: Modifier) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(MeColor(), MeColorLight()),
                startY = 0f,
                endY = 500f
            )
        )
    )
}