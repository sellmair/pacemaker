package io.sellmair.pacemaker.utils

actual object Log {
    actual operator fun invoke(tag: LogTag, level: LogLevel, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.Debug -> android.util.Log.d(tag.name, message, throwable)
            LogLevel.Info -> android.util.Log.i(tag.name, message, throwable)
            LogLevel.Warn -> android.util.Log.w(tag.name, message, throwable)
            LogLevel.Error -> android.util.Log.e(tag.name, message, throwable)
        }
    }
}