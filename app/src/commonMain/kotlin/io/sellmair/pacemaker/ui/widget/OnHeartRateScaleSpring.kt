package io.sellmair.pacemaker.ui.widget

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

fun <T> onHeartRateScaleSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)