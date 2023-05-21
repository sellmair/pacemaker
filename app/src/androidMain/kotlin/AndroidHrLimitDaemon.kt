package io.sellmair.pacemaker.backend

import android.app.Service
import android.content.Context
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import io.sellmair.pacemaker.GroupMember
import io.sellmair.pacemaker.service.GroupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.launchHrLimitDaemon(context: Context, groupService: GroupService) = launch {
    val vibratorManager = context.getSystemService(Service.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    var criticalMemberStates = listOf<GroupMember>()


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
    groupService.group.collect { state ->
        criticalMemberStates = state.members.filter { memberState ->
            val currentHeartRate = memberState.currentHeartRate ?: return@filter false
            val currentHeartRateLimit = memberState.heartRateLimit ?: return@filter false
            currentHeartRate > currentHeartRateLimit
        }
    }
}