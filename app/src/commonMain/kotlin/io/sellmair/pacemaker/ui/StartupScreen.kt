package io.sellmair.pacemaker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pacemaker.app.generated.resources.Res
import pacemaker.app.generated.resources.startup_bluetooth_required
import pacemaker.app.generated.resources.startup_connecting
import pacemaker.app.generated.resources.startup_permission_required
import pacemaker.app.generated.resources.startup_retry_permission
import pacemaker.app.generated.resources.startup_title
import io.sellmair.pacemaker.ui.widget.PacemakerPrimaryButton

enum class StartupStatus {
    Connecting,
    PermissionRequired,
    BluetoothRequired
}

@Composable
fun StartupScreen(
    status: StartupStatus,
    onRetryPermission: (() -> Unit)? = null
) {
    val colors = SharedRhythmColors()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = PacemakerIcons.Mark,
                contentDescription = null,
                tint = if (MaterialTheme.colorScheme.background == SharedRhythmColors.Dark.background) {
                    SharedRhythmColors.Acid
                } else {
                    SharedRhythmColors.Charcoal
                },
                modifier = Modifier.size(width = 96.dp, height = 58.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(Res.string.startup_title),
                style = MaterialTheme.typography.headlineLarge,
                color = colors.text
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    when (status) {
                        StartupStatus.Connecting -> Res.string.startup_connecting
                        StartupStatus.PermissionRequired -> Res.string.startup_permission_required
                        StartupStatus.BluetoothRequired -> Res.string.startup_bluetooth_required
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.muted
            )
            if (status == StartupStatus.Connecting) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colors.accentText,
                    strokeWidth = 2.dp
                )
            }
            if (status == StartupStatus.PermissionRequired && onRetryPermission != null) {
                Spacer(Modifier.height(24.dp))
                PacemakerPrimaryButton(
                    text = stringResource(Res.string.startup_retry_permission),
                    onClick = onRetryPermission
                )
            }
        }
    }
}
