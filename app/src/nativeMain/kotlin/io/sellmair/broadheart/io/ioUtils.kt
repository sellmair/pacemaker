package io.sellmair.broadheart.io

import okio.FileSystem

actual fun defaultFileSystem(): FileSystem {
    return FileSystem.SYSTEM
}