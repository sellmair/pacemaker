package io.sellmair.pacemaker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.sellmair.pacemaker.AndroidPermissionsState

@Composable
fun MissingPermissionsOverlayScreen() {
    val permissionsState = AndroidPermissionsState.collectAsState().value ?: return
    if(permissionsState is AndroidPermissionsState.Granted) return
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MeColorWhite().copy(alpha = 0.3f))
            .clickable(enabled = false) {}
    ) {
        
        Text("Some necessary permissions were not granted")
    }
}