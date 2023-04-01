package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerIfAny(onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
}