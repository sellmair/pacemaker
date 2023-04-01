package io.sellmair.pacemaker.utils

data class LogTag(val name: String) {
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

