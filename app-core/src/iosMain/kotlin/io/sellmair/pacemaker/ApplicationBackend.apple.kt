package io.sellmair.pacemaker

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope


@OptIn(ExperimentalForeignApi::class)
actual fun ApplicationBackend.launchPlatform(scope: CoroutineScope) {
    scope.launchVibrationWarningActor()
    scope.launchSpeechSynthesizer()
}