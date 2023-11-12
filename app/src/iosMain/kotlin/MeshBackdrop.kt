@file:Suppress("PackageDirectoryMismatch")

package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
internal actual fun MeshBackdrop(modifier: Modifier) {
    Box(
        modifier = modifier.background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF667EEA), Color(0xFF64B6FF)),
                    startY = 0f,
                    endY = 500f
                )
            )
    )
}