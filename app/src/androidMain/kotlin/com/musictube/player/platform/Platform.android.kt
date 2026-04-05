package com.musictube.player.platform

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

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

actual suspend fun extractYouTubeAudioViaNewPipe(videoId: String): String? {
    return try {
        withContext(Dispatchers.IO) {
            Log.d("YT", "NewPipe: trying $videoId")
            val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
            val best = info.audioStreams.maxByOrNull { it.averageBitrate }
            val url = best?.content
            if (url != null) Log.d("YT", "NewPipe: success for $videoId") else Log.w("YT", "NewPipe: no URL for $videoId")
            url
        }
    } catch (e: Exception) {
        Log.w("YT", "NewPipe failed for $videoId: ${e.message}")
        null
    }
}
