package io.sellmair.pacemaker

import kotlinx.coroutines.CoroutineScope

actual fun ApplicationBackend.launchPlatform(scope: CoroutineScope) {
    scope.launchVibrationWarningActor()
}