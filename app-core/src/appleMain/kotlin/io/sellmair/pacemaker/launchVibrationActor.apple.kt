package io.sellmair.pacemaker

import io.sellmair.pacemaker.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType.UINotificationFeedbackTypeError
import kotlin.time.Duration.Companion.milliseconds

internal fun CoroutineScope.launchVibrationWarningActor() = launch {
    while(isActive) {
        delay(500.milliseconds)
        if(CriticalGroupState.get().value != null) {
            UINotificationFeedbackGenerator().notificationOccurred(UINotificationFeedbackTypeError)
        }
    }
}