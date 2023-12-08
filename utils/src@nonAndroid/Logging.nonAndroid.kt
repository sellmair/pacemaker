package io.sellmair.pacemaker.utils

actual object Log {
    actual operator fun invoke(tag: LogTag, level: LogLevel, message: String, throwable: Throwable?) {
        println(
            "[$tag | $level]: $message" + if (throwable != null) {
                "\n${throwable.stackTraceToString()}"
            } else ""
        )
    }
}