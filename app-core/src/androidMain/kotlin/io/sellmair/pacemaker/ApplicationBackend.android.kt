package io.sellmair.pacemaker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual fun ApplicationBackend.launchPlatform(scope: CoroutineScope) {
    this as AndroidApplicationBackend
    scope.launchVibrationWarningActor(this)
    scope.launchTextToSpeech(this)
}