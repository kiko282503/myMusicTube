package com.musictube.player.service

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeStreamService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun extractAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        Log.d("YT", "Extracting audio for: $videoId")
        fetchWithCobalt(videoId) ?: fetchWithPiped(videoId) ?: fetchWithInvidious(videoId)
    }

    private fun fetchWithCobalt(videoId: String): String? {
        return try {
            Log.d("YT", "Cobalt: trying...")
            val ytUrl = "https://www.youtube.com/watch?v=$videoId"
            val reqBody = """{"url":"$ytUrl","downloadMode":"audio","audioFormat":"best"}"""
            val request = Request.Builder()
                .url("https://api.cobalt.tools/")
                .post(reqBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "MusicTube/1.0")
                .build()
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            if (!response.isSuccessful) {
                Log.w("YT", "Cobalt: HTTP ${response.code} - ${body?.take(120)}")
                return null
            }
            if (body == null) return null
            val json = JsonParser.parseString(body).asJsonObject
            val status = json.get("status")?.asString
            Log.d("YT", "Cobalt status: $status")
            if (status == "redirect" || status == "tunnel" || status == "stream") {
                val url = json.get("url")?.asString
                if (url != null) Log.d("YT", "Cobalt: got URL")
                return url
            }
            Log.w("YT", "Cobalt: unexpected status=$status body=${body.take(200)}")
            null
        } catch (e: Exception) {
            Log.e("YT", "Cobalt error: ${e.message}")
            null
        }
    }

    private val pipedInstances = listOf(
        "pipedapi.tokhmi.xyz",
        "pipedapi.in.projectsegfau.lt",
        "pipedapi.moomoo.me",
        "pipedapi.syncpundit.io"
    )

    private fun fetchWithPiped(videoId: String): String? {
        for (instance in pipedInstances) {
            try {
                Log.d("YT", "Piped: trying $instance")
                val request = Request.Builder()
                    .url("https://$instance/streams/$videoId")
                    .addHeader("User-Agent", "MusicTube/1.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Log.w("YT", "Piped $instance: HTTP ${response.code}")
                    continue
                }
                if (!body.trimStart().startsWith("{")) {
                    Log.w("YT", "Piped $instance: non-JSON response")
                    continue
                }
                val root = JsonParser.parseString(body).asJsonObject
                val audioStreams = root.getAsJsonArray("audioStreams")
                if (audioStreams == null) { Log.w("YT", "Piped $instance: no audioStreams"); continue }
                var bestUrl: String? = null
                var bestBitrate = 0
                for (el in audioStreams) {
                    val s = el.asJsonObject
                    val url = s.get("url")?.asString
                    if (url == null) continue
                    val bitrate = try { s.get("bitrate")?.asInt ?: 0 } catch (e: Exception) { 0 }
                    if (bitrate > bestBitrate) { bestBitrate = bitrate; bestUrl = url }
                }
                if (bestUrl != null) { Log.d("YT", "Piped $instance: success"); return bestUrl }
            } catch (e: Exception) {
                Log.w("YT", "Piped $instance: ${e.message}")
            }
        }
        return null
    }

    private val invidiousInstances = listOf(
        "invidious.slipfox.xyz",
        "inv.tux.pizza",
        "yt.artemislena.eu",
        "invidious.privacyredirect.com",
        "inv.nadeko.net"
    )

    private fun fetchWithInvidious(videoId: String): String? {
        for (instance in invidiousInstances) {
            try {
                Log.d("YT", "Invidious: trying $instance")
                val request = Request.Builder()
                    .url("https://$instance/api/v1/videos/$videoId")
                    .addHeader("User-Agent", "MusicTube/1.0")
                    .build()
                val response = httpClient.newCall(request).execute()
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Log.w("YT", "Invidious $instance: HTTP ${response.code}")
                    continue
                }
                if (!body.trimStart().startsWith("{")) {
                    Log.w("YT", "Invidious $instance: non-JSON")
                    continue
                }
                val root = JsonParser.parseString(body).asJsonObject
                val formats = root.getAsJsonArray("adaptiveFormats")
                if (formats == null) { Log.w("YT", "Invidious $instance: no adaptiveFormats"); continue }
                var bestUrl: String? = null
                var bestBitrate = 0
                for (el in formats) {
                    val fmt = el.asJsonObject
                    val mimeType = fmt.get("type")?.asString
                    if (mimeType == null || !mimeType.startsWith("audio/")) continue
                    val url = fmt.get("url")?.asString
                    if (url == null) continue
                    val bitrate = try { fmt.get("bitrate")?.asInt ?: 0 } catch (e: Exception) { 0 }
                    if (bitrate > bestBitrate) { bestBitrate = bitrate; bestUrl = url }
                }
                if (bestUrl != null) { Log.d("YT", "Invidious $instance: success"); return bestUrl }
                Log.w("YT", "Invidious $instance: no audio URL found")
            } catch (e: Exception) {
                Log.w("YT", "Invidious $instance: ${e.message}")
            }
        }
        Log.e("YT", "All strategies failed for $videoId")
        return null
    }
}
