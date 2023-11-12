package io.sellmair.pacemaker.utils

import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

fun FileSystem.readUtf8OrNull(path: Path): String? {
    return try {
        source(path).buffer().use { it.readUtf8() }
    } catch (t: Throwable) {
        null
    }
}
