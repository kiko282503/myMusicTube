package com.musictube.player.platform

import android.util.Log

actual fun logDebug(tag: String, message: String) { Log.d(tag, message) }
actual fun logWarning(tag: String, message: String) { Log.w(tag, message) }
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun platformDeleteFile(path: String) {
    try {
        java.io.File(path).delete()
    } catch (e: Exception) {
        Log.w("platformDeleteFile", "Failed to delete $path", e)
    }
}

actual fun platformUuid(): String = java.util.UUID.randomUUID().toString()
