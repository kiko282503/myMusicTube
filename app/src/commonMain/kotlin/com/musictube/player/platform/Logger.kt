package com.musictube.player.platform

/** Minimal platform-neutral logging helpers and utility functions, backed by platform actuals. */
expect fun logDebug(tag: String, message: String)
expect fun logWarning(tag: String, message: String)
expect fun logError(tag: String, message: String, throwable: Throwable? = null)

expect fun currentTimeMillis(): Long
expect fun platformDeleteFile(path: String)
expect fun platformUuid(): String
