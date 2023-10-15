package io.sellmair.pacemaker.utils

fun Context(vararg elements: Context.Keyed): Context {
    return Context.create(
        elements.map { keyed ->
            @Suppress("UNCHECKED_CAST")
            when (keyed) {
                is Context.KeyedValue<*> -> keyed
                is Context.Element<*> -> Context.KeyedValue(keyed.key as Context.Key<Any>, keyed)
            }
        }
    )
}

class Context private constructor(
    private val elements: Map<Key<*>, Any>
) {
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

    operator fun contains(key: Key<*>): Boolean {
        return key in elements
    }

    operator fun <T : Any> get(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        elements[key]?.let { return it as T }
        if (key is Key.WithDefault<T>) {
            return key.default
        }
        return null
    }

    operator fun <T : Any> get(key: Key.WithDefault<T>): T {
        @Suppress("UNCHECKED_CAST")
        return elements[key]?.let { return it as T } ?: key.default
    }

    fun <T : Any> plus(key: Key<T>, value: T): Context {
        val newElements = elements.plus(key to value)
        return Context(newElements)
    }

    operator fun <T : Element<T>> plus(element: T): Context {
        return plus(element.key, element)
    }

    operator fun plus(other: Context): Context {
        return Context(this.elements + other.elements)
    }

    companion object {
        val empty = Context(emptyMap())
        internal fun create(elements: Iterable<KeyedValue<*>>) = Context(elements.associate { it.key to it.value })
    }
}

internal infix fun <T : Any> Context.Key<T>.plus(value: T) = Context.KeyedValue(this, value)