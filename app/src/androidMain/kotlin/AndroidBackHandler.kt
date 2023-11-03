package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerIfAny(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}