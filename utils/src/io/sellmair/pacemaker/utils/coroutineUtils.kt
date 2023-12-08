package io.sellmair.pacemaker.utils

import kotlin.coroutines.suspendCoroutine

/**
 * This function will return 'never'
 */
suspend fun never(): Nothing {
    suspendCoroutine<Nothing> {}
}
