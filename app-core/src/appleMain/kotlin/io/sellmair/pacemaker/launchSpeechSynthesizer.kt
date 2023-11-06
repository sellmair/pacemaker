@file:OptIn(ExperimentalForeignApi::class)

package io.sellmair.pacemaker

import UtteranceEvent
import io.sellmair.pacemaker.utils.events
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeVoicePrompt
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate
import platform.AVFAudio.setActive
import kotlin.time.Duration.Companion.milliseconds

private val synthesizer = AVSpeechSynthesizer()

@OptIn(ExperimentalForeignApi::class)
internal fun CoroutineScope.launchSpeechSynthesizer() = launch {
    val audioSession = AVAudioSession.sharedInstance()
    audioSession.setCategory(
        AVAudioSessionCategoryPlayback,
        AVAudioSessionCategoryOptionDuckOthers or AVAudioSessionCategoryOptionInterruptSpokenAudioAndMixWithOthers,
        null
    )
    audioSession.setMode(AVAudioSessionModeVoicePrompt, null)

    events<UtteranceEvent>().conflate().collect { event ->
        if (!UtteranceState.shouldBeAnnounced(event)) return@collect
        val utterance = AVSpeechUtterance.speechUtteranceWithString(event.message)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        synthesizer.speakUtterance(utterance)
        while (synthesizer.isSpeaking()) delay(250.milliseconds)
        audioSession.setActive(false, null)
    }
}
