@file:Suppress("PackageDirectoryMismatch")

package io.sellmair.pacemaker.ui.settingsPage

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.sellmair.pacemaker.R

@Composable
actual fun CatImage(modifier: Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(R.drawable.cat),
        contentDescription = "This is an image of a cat, because no devices were found 🤷"
    )
}