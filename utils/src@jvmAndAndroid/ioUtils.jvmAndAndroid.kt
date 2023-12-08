package io.sellmair.pacemaker.utils

import okio.FileSystem

/* https://youtrack.jetbrains.com/issue/KTIJ-25140/Okio-False-positive-MISSINGDEPENDENCYSUPERCLASS-on-FileSystem.SYSTEM */
@Suppress("MISSING_DEPENDENCY_SUPERCLASS")
actual fun defaultFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}