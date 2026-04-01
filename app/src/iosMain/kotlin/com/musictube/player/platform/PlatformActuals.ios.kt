@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.musictube.player.platform

import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

actual fun logDebug(tag: String, message: String) {
    println("[$tag/D] $message")
}

actual fun logWarning(tag: String, message: String) {
    println("[$tag/W] $message")
}

actual fun logError(tag: String, message: String, throwable: Throwable?) {
    println(if (throwable != null) "[$tag/E] $message: $throwable" else "[$tag/E] $message")
}

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun platformDeleteFile(path: String) {
    try {
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    } catch (_: Exception) { }
}

actual fun platformUuid(): String = NSUUID().uUIDString
