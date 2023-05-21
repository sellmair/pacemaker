package io.sellmair.pacemaker.ui

import androidx.compose.runtime.Composable

@Composable
internal expect fun BackHandlerIfAny(onBack: () -> Unit)
