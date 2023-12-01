package io.sellmair.pacemaker

import android.content.Context
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

internal class AndroidContextProvider(val context: Context) : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key

    companion object Key : CoroutineContext.Key<AndroidContextProvider>
}

suspend fun androidContext(): Context {
    val provider = currentCoroutineContext()[AndroidContextProvider] ?: error("Missing Android 'Context'")
    return provider.context
}

