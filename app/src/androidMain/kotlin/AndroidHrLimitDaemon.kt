package io.sellmair.pacemaker.backend

import android.app.Service
import android.content.Context
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.GroupMember
import io.sellmair.pacemaker.service.GroupService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes

fun CoroutineScope.launchHrLimitDaemon(context: Context, groupService: GroupService) = launch {
    val vibratorManager = context.getSystemService(Service.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    val textToSpeech = async {
        TextToSpeech(context)?.apply {
            language = Locale.US
        }
    }


    var group: Group? = null
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

    /* Text To Speech: Tell user who is over the limit */
    @Suppress("DEPRECATION")
    launch {
        while (true) {
            delay(5000)
            if (criticalMemberStates.isNotEmpty()) {
                launch textToSpeech@{
                    val speaker = textToSpeech.await() ?: return@textToSpeech
                    if (speaker.isSpeaking) return@textToSpeech
                    val message = "Slow down! ${
                        criticalMemberStates.joinToString(", ") {
                            "${it.user?.name} is at ${it.currentHeartRate?.value?.roundToInt()} bpm"
                        }
                    }"

                    speaker.speak(message, TextToSpeech.QUEUE_FLUSH, null)
                }
            }
        }
    }

    /* Text To Speech: Tell heart rate every minute */
    launch {
        while (true) {
            delay(1.minutes)
            launch textToSpeech@{
                val speaker = textToSpeech.await() ?: return@textToSpeech
                val me = group?.members.orEmpty().firstOrNull { it.user?.isMe == true }
                val heartRate = me?.currentHeartRate?.value?.roundToInt() ?: return@textToSpeech
                val limit = me.heartRateLimit?.value?.roundToInt() ?: return@textToSpeech
                val message = "Your heart rate is at: $heartRate. The current limit is: $limit"
                speaker.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }

    /* Collect critical member states */
    groupService.group.collect { state ->
        group = state
        criticalMemberStates = state.members.filter { memberState ->
            val currentHeartRate = memberState.currentHeartRate ?: return@filter false
            val currentHeartRateLimit = memberState.heartRateLimit ?: return@filter false
            currentHeartRate > currentHeartRateLimit
        }
    }
}


suspend fun TextToSpeech(context: Context): TextToSpeech? {
    return suspendCoroutine { continuation ->
        lateinit var textToSpeech: TextToSpeech
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                continuation.resume(textToSpeech)
            } else continuation.resume(null)
        }
    }
}