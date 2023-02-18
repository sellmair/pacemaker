package io.sellmair.broadheart.service

import android.app.Service
import android.content.Context
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.launchHrLimitDaemon(context: Context, groupService: GroupService) = launch {
    val vibratorManager = context.getSystemService(Service.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    var criticalMemberStates = listOf<GroupMemberState>()


    /* Vibrate on any critical member state */
    launch {
        while (true) {
            delay(1000)
            if (criticalMemberStates.isNotEmpty()) {
                vibratorManager.vibrate(
                    CombinedVibration.createParallel(
                        VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                )
            }
        }
    }

    /* Collect critical member states */
    groupService.groupState.collect { state ->
        criticalMemberStates = state.members.filter { memberState ->
            memberState.currentHeartRate != null && memberState.upperHeartRateLimit != null
                    && memberState.currentHeartRate > memberState.upperHeartRateLimit
        }
    }
}