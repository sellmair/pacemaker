package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import io.sellmair.pacemaker.R

@Composable
internal actual fun MeshBackdrop(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(R.drawable.mesh1),
        contentDescription = "Backdrop",
        contentScale = ContentScale.Crop
    )
}