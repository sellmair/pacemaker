package io.sellmair.pacemaker

import io.sellmair.pacemaker.ui.launchHeartRateUtteranceActor
import kotlinx.coroutines.CoroutineScope

fun CoroutineScope.launchFrontendServices() {
    launchHeartRateUtteranceActor()
    launchPlatformSpecificFrontendServices()
}

expect fun CoroutineScope.launchPlatformSpecificFrontendServices()

