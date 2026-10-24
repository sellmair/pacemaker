package io.sellmair.pacemaker.utils

import okio.FileSystem

actual fun defaultFileSystem(): FileSystem = FileSystem.SYSTEM
