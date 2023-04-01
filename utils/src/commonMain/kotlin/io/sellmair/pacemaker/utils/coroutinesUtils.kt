package io.sellmair.pacemaker.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> Flow<T>.distinct(key: (T) -> Any? = { it }): Flow<T> = flow {
    val values = hashSetOf<Any?>()
    collect { value ->
        if (values.add(key(value))) {
            emit(value)
        }
    }
}
