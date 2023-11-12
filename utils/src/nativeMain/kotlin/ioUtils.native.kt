package io.sellmair.pacemaker.utils

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSUInteger


@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, BetaInteropApi::class)
fun ByteArray.toNSData(): NSData {
    return memScoped {
        NSData.create(bytes = this@toNSData.toCValues().ptr, length = size.convert<NSUInteger>())
    }
}