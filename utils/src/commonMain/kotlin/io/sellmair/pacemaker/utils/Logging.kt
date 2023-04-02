package io.sellmair.pacemaker.utils

import kotlin.reflect.KClass

data class LogTag(val name: String) {
    fun with(additional: String) = LogTag("$name | $additional")
    fun forClass(clazz: KClass<*>) = with(clazz.simpleName.orEmpty())
    inline fun <reified T> forClass() = forClass(T::class)

    override fun toString(): String {
        return name
    }

    companion object
}

enum class LogLevel {
    Debug, Info, Warn, Error
}

expect object Log {
    operator fun invoke(tag: LogTag, level: LogLevel, message: String, throwable: Throwable? = null)
}

fun LogTag.debug(message: String, throwable: Throwable? = null) = Log(this, LogLevel.Debug, message, throwable)
fun LogTag.info(message: String, throwable: Throwable? = null) = Log(this, LogLevel.Info, message, throwable)
fun LogTag.warn(message: String, throwable: Throwable? = null) = Log(this, LogLevel.Warn, message, throwable)
fun LogTag.error(message: String, throwable: Throwable? = null) = Log(this, LogLevel.Error, message, throwable)

