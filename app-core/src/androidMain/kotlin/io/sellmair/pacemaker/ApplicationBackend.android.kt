package io.sellmair.pacemaker

import kotlinx.coroutines.CoroutineScope

actual fun ApplicationBackend.launchPlatform(scope: CoroutineScope) {
    this as AndroidApplicationBackend
    scope.launchVibrationWarningActor(this)
}