package com.musictube.player.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Extracts YouTube audio stream URLs via the Piped API (open-source YouTube proxy).
 */
class YouTubeAudioExtractor {

    companion object {
        private const val TAG = "YouTubeAudioExtractor"
        private val PIPED_INSTANCES = listOf(
            "https://pipedapi.kavin.rocks",
            "https://piped-api.garudalinux.org",
            "https://api.piped.projectsegfau.lt",
            "https://pipedapi.bockhacker.me",
            "https://piped-api.codeberg.page"
        )
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun extractAudioUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            for (instance in PIPED_INSTANCES) {
                try {
                    val url = fetchFromPiped(instance, videoId)
                    if (url != null) {
                        Log.i(TAG, "Piped extraction SUCCESS from $instance for: $videoId")
                        return@withContext url
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Piped instance $instance failed for $videoId: ${e.message}")
                }
            }
            Log.e(TAG, "All Piped instances failed for: $videoId")
            null
        }
    }

    private fun fetchFromPiped(instance: String, videoId: String): String? {
        val request = Request.Builder()
            .url("$instance/streams/$videoId")
            .addHeader("User-Agent", "Mozilla/5.0")
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null
        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val audioStreams = json.optJSONArray("audioStreams") ?: return null
        var bestUrl: String? = null
        var bestBitrate = 0
        for (i in 0 until audioStreams.length()) {
            val stream = audioStreams.getJSONObject(i)
            val bitrate = stream.optInt("bitrate", 0)
            val mimeType = stream.optString("mimeType", "")
            val url = stream.optString("url", "")
            if (url.isNotBlank() && (mimeType.contains("audio") || mimeType.contains("mp4"))) {
                if (bitrate > bestBitrate) { bestBitrate = bitrate; bestUrl = url }
            }
        }
        return bestUrl
    }
}
