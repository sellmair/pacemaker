package io.sellmair.pacemaker

import android.content.Context
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.content.getSystemService
import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal fun CoroutineScope.launchVibrationWarningActor(context: Context) = launch {
    val vibratorManager = context.getSystemService<VibratorManager>() ?: return@launch
    while (isActive) {
        delay(1.seconds)
        if (CriticalGroupState.get().value != null && UtteranceState.get().value >= UtteranceState.Warnings) {
            vibratorManager.vibrate(
                CombinedVibration.createParallel(
                    VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            )
        }
    }
}
