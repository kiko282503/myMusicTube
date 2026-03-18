package com.musictube.player.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts YouTube audio stream URLs via the Piped API (open-source YouTube proxy).
 * Piped does server-side extraction and returns usable audio stream URLs.
 * Multiple instances are tried in order for reliability.
 */
@Singleton
class YouTubeAudioExtractor @Inject constructor() {

    companion object {
        private const val TAG = "YouTubeAudioExtractor"

        // Public Piped API instances - tried in order until one works
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

    /**
     * Extract best audio stream URL for a YouTube video ID.
     * Tries multiple Piped API instances until one succeeds.
     */
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

    /** Same as extractAudioUrl - Piped handles YouTube Music video IDs too */
    suspend fun extractFromYouTubeMusic(videoId: String): String? = extractAudioUrl(videoId)

    private fun fetchFromPiped(instance: String, videoId: String): String? {
        val request = Request.Builder()
            .url("$instance/streams/$videoId")
            .addHeader("User-Agent", "MusicTube/1.0 (Android)")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w(TAG, "Piped $instance returned ${response.code} for $videoId")
            return null
        }

        val body = response.body?.string() ?: return null
        val json = JSONObject(body)

        val audioStreams = json.optJSONArray("audioStreams") ?: return null
        if (audioStreams.length() == 0) return null

        // Pick the highest bitrate audio-only stream
        var bestUrl: String? = null
        var bestBitrate = -1

        for (i in 0 until audioStreams.length()) {
            val stream = audioStreams.getJSONObject(i)
            val bitrate = stream.optInt("bitrate", 0)
            val streamUrl = stream.optString("url", "")
            val mimeType = stream.optString("mimeType", "")

            // Prefer audio-only streams (not video)
            if (streamUrl.isNotEmpty() && bitrate > bestBitrate && !mimeType.startsWith("video")) {
                bestBitrate = bitrate
                bestUrl = streamUrl
            }
        }

        Log.d(TAG, "Best audio stream bitrate: ${bestBitrate}bps for $videoId")
        return bestUrl
    }
}
