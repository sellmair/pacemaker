package io.sellmair.broadheart.utils

import okio.FileSystem

actual fun defaultFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}