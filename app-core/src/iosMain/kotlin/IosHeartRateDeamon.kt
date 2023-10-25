import io.sellmair.pacemaker.Group
import io.sellmair.pacemaker.GroupService
import io.sellmair.pacemaker.UserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val textToSpeech = AVSpeechSynthesizer()
fun CoroutineScope.launchHrLimitDaemon(groupService: GroupService) = launch {


    var group: Group? = null
    var criticalUserStates = listOf<UserState>()

    /* Text To Speech: Tell user who is over the limit */
    @Suppress("DEPRECATION")
    launch {
        while (true) {
            announce("Hello")

            delay(15.seconds)
            val criticalStates = criticalUserStates.toList()
            if (criticalStates.isNotEmpty()) {
                launch textToSpeech@{
                    if (textToSpeech.isSpeaking()) return@textToSpeech
                    val message = "Slow down! ${
                        if (criticalStates.singleOrNull()?.isMe == true) {
                            "You are at " +
                                "${criticalStates.singleOrNull()?.heartRate?.value?.roundToInt()} bpm"
                        } else criticalStates.joinToString(", ") {
                            "${it.user.name} is at ${it.heartRate.value.roundToInt()} bpm"
                        }
                    }"

                    announce(message)
                }
            }
        }
    }

    /* Text To Speech: Tell heart rate every minute */
    launch {
        while (true) {
            delay(1.minutes)
            launch textToSpeech@{
                val me = group?.members.orEmpty().firstOrNull { it.isMe }
                val heartRate = me?.heartRate?.value?.roundToInt() ?: return@textToSpeech
                val limit = me.heartRateLimit?.value?.roundToInt() ?: return@textToSpeech
                val message = "Your heart rate is at: $heartRate. The current limit is: $limit"
                announce(message)
            }
        }
    }

    /* Collect critical member states */
    groupService.group.collect { state ->
        group = state
        criticalUserStates = state.members.filter { memberState ->
            val currentHeartRate = memberState.heartRate
            val currentHeartRateLimit = memberState.heartRateLimit ?: return@filter false
            currentHeartRate > currentHeartRateLimit
        }
    }
}

private fun announce(message: String) {
    val utterance = AVSpeechUtterance.speechUtteranceWithString(message)
    println(AVSpeechSynthesisVoice.voiceWithIdentifier("com.apple.speech.synthesis.voice.Fred"))
    utterance.voice = AVSpeechSynthesisVoice.voiceWithIdentifier("com.apple.speech.synthesis.voice.Fred")
    utterance.rate = 0.53f
    textToSpeech.speakUtterance(utterance)
}

