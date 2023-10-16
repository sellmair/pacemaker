package io.sellmair.pacemaker.utils

fun Configuration(vararg elements: Configuration.Keyed): Configuration {
    return ConfigurationImpl.create(
        elements.map { keyed ->
            @Suppress("UNCHECKED_CAST")
            when (keyed) {
                is Configuration.KeyedValue<*> -> keyed
                is Configuration.Element<*> -> Configuration.KeyedValue(keyed.key as Configuration.Key<Any>, keyed)
            }
        }
    )
}

interface Configuration {
    interface Key<T : Any> {
        interface WithDefault<T : Any> : Key<T> {
            val default: T
        }
    }

    sealed interface Keyed

    data class KeyedValue<T : Any>(val key: Key<T>, val value: T) : Keyed

    interface Element<T> : Keyed where T : Element<T> {
        val key: Key<T>
    }

    data class Entry<T : Any>(val key: Key<T>, val value: T)

    val entries: Set<Entry<*>>
    val isEmpty: Boolean
    operator fun contains(key: Key<*>): Boolean
    operator fun <T : Any> get(key: Key<T>): T?
    operator fun <T : Any> get(key: Key.WithDefault<T>): T
    fun <T : Any> plus(key: Key<T>, value: T): Configuration
    fun <T : Element<T>> plus(element: T): Configuration
    operator fun plus(other: Configuration): Configuration

    companion object {
        val empty: Configuration = Configuration()
    }
}

fun Configuration.currentConfiguration() = this

private class ConfigurationImpl private constructor(
    private val elements: Map<Configuration.Key<*>, Any>
) : Configuration {

    @Suppress("UNCHECKED_CAST")
    override val entries: Set<Configuration.Entry<*>> =
        elements.map { (key, value) -> Configuration.Entry(key as Configuration.Key<Any>, value) }.toSet()

    override val isEmpty: Boolean = elements.isEmpty()

    override fun contains(key: Configuration.Key<*>): Boolean {
        return key in elements
    }

    override fun <T : Any> get(key: Configuration.Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        elements[key]?.let { return it as T }
        if (key is Configuration.Key.WithDefault<T>) {
            return key.default
        }
        return null
    }

    override fun <T : Any> get(key: Configuration.Key.WithDefault<T>): T {
        @Suppress("UNCHECKED_CAST")
        return elements[key]?.let { return it as T } ?: key.default
    }

    override fun <T : Any> plus(key: Configuration.Key<T>, value: T): Configuration {
        val newElements = elements.plus(key to value)
        return ConfigurationImpl(newElements)
    }

    override fun <T : Configuration.Element<T>> plus(element: T): Configuration {
        return plus(element.key, element)
    }

    override fun plus(other: Configuration): Configuration {
        return if (other.isEmpty) return this
        else if (this.isEmpty) return other
        else if (other is ConfigurationImpl) ConfigurationImpl(this.elements + other.elements)
        else ConfigurationImpl(this.elements + other.entries.associate { it.key to it.value })
    }

    companion object {
        internal fun create(elements: Iterable<Configuration.KeyedValue<*>>) =
            ConfigurationImpl(elements.associate { it.key to it.value })
    }
}

internal infix fun <T : Any> Configuration.Key<T>.plus(value: T) = Configuration.KeyedValue(this, value)