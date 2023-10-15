package io.sellmair.pacemaker

import android.app.Service
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.content.getSystemService
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
import kotlin.time.Duration.Companion.seconds


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
            delay(15.seconds)
            val criticalStates = criticalMemberStates.toList()
            if (criticalStates.isNotEmpty()) {
                launch textToSpeech@{
                    val speaker = textToSpeech.await() ?: return@textToSpeech
                    if (speaker.isSpeaking) return@textToSpeech
                    val message = "Slow down! ${
                        if (criticalStates.singleOrNull()?.user?.isMe == true) {
                            "You are at " +
                                "${criticalStates.singleOrNull()?.currentHeartRate?.value?.roundToInt()} bpm"
                        } else criticalStates.joinToString(", ") {
                            "${it.user?.name} is at ${it.currentHeartRate?.value?.roundToInt()} bpm"
                        }
                    }"

                    announce(context, speaker, message)
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
                announce(context, speaker, message)
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

private suspend fun announce(context: Context, textToSpeech: TextToSpeech, message: String) {
    val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANT)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    textToSpeech.setAudioAttributes(audioAttributes)

    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(audioAttributes)
        .setAcceptsDelayedFocusGain(false)
        .setWillPauseWhenDucked(false)
        .build()

    val audioManager = context.getSystemService<AudioManager>()

    audioManager?.requestAudioFocus(focusRequest)
    textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null)

    do {
        delay(100)
    } while (textToSpeech.isSpeaking)

    audioManager?.abandonAudioFocusRequest(focusRequest)
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