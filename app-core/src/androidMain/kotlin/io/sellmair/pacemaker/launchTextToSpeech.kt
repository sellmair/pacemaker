package io.sellmair.pacemaker

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import androidx.core.content.getSystemService
import io.sellmair.pacemaker.utils.collectEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal fun CoroutineScope.launchTextToSpeech(context: Context) = launch {
    val textToSpeech = TextToSpeech(context) ?: return@launch
    textToSpeech.setSpeechRate(1.3f)
    textToSpeech.setPitch(0.85f)

    collectEvents<UtteranceEvent> { event ->
        if (!UtteranceState.shouldBeAnnounced(event)) return@collectEvents
        if (textToSpeech.isSpeaking) return@collectEvents

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
        textToSpeech.speak(event.message, TextToSpeech.QUEUE_FLUSH, null)

        do {
            delay(100)
        } while (textToSpeech.isSpeaking)

        audioManager?.abandonAudioFocusRequest(focusRequest)
    }
}


private suspend fun TextToSpeech(context: Context): TextToSpeech? {
    return suspendCoroutine { continuation ->
        lateinit var textToSpeech: TextToSpeech
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                continuation.resume(textToSpeech)
            } else continuation.resume(null)
        }
    }
}