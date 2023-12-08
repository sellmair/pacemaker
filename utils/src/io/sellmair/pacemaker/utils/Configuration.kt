package io.sellmair.pacemaker.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.CoroutineContext

suspend fun <T : Any> ConfigurationKey<T>.value(): T? {
    currentCoroutineContext()[this]?.let { return it.value }
    if (this is ConfigurationKey.WithDefault) return default
    return null
}

suspend fun <T : Any> ConfigurationKey.WithDefault<T>.value(): T {
    currentCoroutineContext()[this]?.let { return it.value }
    return default
}

operator fun <T : Any> ConfigurationKey<T>.invoke(value: T): ConfigurationElement<T> {
    return ConfigurationElement(this, value)
}


data class ConfigurationElement<T : Any>(
    override val key: ConfigurationKey<T>, val value: T
) : CoroutineContext.Element


interface ConfigurationKey<T : Any> : CoroutineContext.Key<ConfigurationElement<T>> {
    interface WithDefault<T : Any> : ConfigurationKey<T> {
        val default: T
    }
}
