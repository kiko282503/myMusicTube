package com.musictube.player.service

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

@Singleton
class YouTubeStreamService @Inject constructor() {

    private data class CachedUrl(val url: String, val fetchedAt: Long = System.currentTimeMillis()) {
        fun isValid(): Boolean = System.currentTimeMillis() - fetchedAt < 5 * 60 * 60 * 1000L // 5 hours
    }

    // Cache resolved stream URLs so repeat plays are instant and prefetch results are reused
    private val urlCache = ConcurrentHashMap<String, CachedUrl>()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun extractAudioUrl(videoId: String): String? {
        urlCache[videoId]?.takeIf { it.isValid() }?.let {
            Log.d("YT", "Cache hit for $videoId")
            return it.url
        }
        Log.d("YT", "Extracting audio for: $videoId (parallel race)")
        val result = channelFlow {
            launch(Dispatchers.IO) { fetchWithYouTubeiAndroid(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithNewPipe(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithPiped(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithInvidious(videoId)?.let { send(it) } }
        }.firstOrNull()
        result?.let { urlCache[videoId] = CachedUrl(it) }
        return result
    }

    /** Silently warms the cache for [videoId] without returning the URL. Safe to call in background. */
    suspend fun prefetchAudioUrl(videoId: String) {
        if (urlCache[videoId]?.isValid() == true) return
        extractAudioUrl(videoId)
    }

    private fun fetchWithYouTubeiAndroid(videoId: String): String? {
        return try {
            Log.d("YT", "YouTubei Android: trying $videoId")
            val body = """{
                "context": {
                    "client": {
                        "clientName": "ANDROID",
                        "clientVersion": "19.17.34",
                        "androidSdkVersion": 34,
                        "hl": "en",
                        "gl": "US"
                    }
                },
                "videoId": "$videoId",
                "contentCheckOk": true,
                "racyCheckOk": true
            }""".trimIndent()

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "com.google.android.youtube/19.17.34 (Linux; U; Android 14)")
                .addHeader("X-YouTube-Client-Name", "3")
                .addHeader("X-YouTube-Client-Version", "19.17.34")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "YouTubei Android HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) {
                Log.d("YT", "YouTubei Android: success")
                return directUrl
            }

            Log.w("YT", "YouTubei Android: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "YouTubei Android failed: ${e.message}")
            null
        }
    }

    private fun extractAudioUrlFromPlayerJson(root: com.google.gson.JsonObject): String? {
        val streamingData = root.getAsJsonObject("streamingData") ?: return null

        val candidates = mutableListOf<com.google.gson.JsonObject>()
        streamingData.getAsJsonArray("adaptiveFormats")?.forEach { candidates.add(it.asJsonObject) }
        streamingData.getAsJsonArray("formats")?.forEach { candidates.add(it.asJsonObject) }

        var bestUrl: String? = null
        var bestBitrate = -1

        for (fmt in candidates) {
            val mimeType = fmt.get("mimeType")?.asString ?: ""
            if (!mimeType.startsWith("audio/")) continue

            val bitrate = try { fmt.get("bitrate")?.asInt ?: 0 } catch (_: Exception) { 0 }
            val direct = fmt.get("url")?.asString
            val fromCipher = fmt.get("signatureCipher")?.asString
                ?.let { parseUrlFromSignatureCipher(it) }

            val url = direct ?: fromCipher ?: continue
            if (bitrate > bestBitrate) {
                bestBitrate = bitrate
                bestUrl = url
            }
        }

        return bestUrl
    }

    private fun parseUrlFromSignatureCipher(cipher: String): String? {
        return try {
            cipher.split("&")
                .mapNotNull { part ->
                    val idx = part.indexOf('=')
                    if (idx <= 0) return@mapNotNull null
                    val key = part.substring(0, idx)
                    val value = part.substring(idx + 1)
                    key to value
                }
                .toMap()["url"]
                ?.let { URLDecoder.decode(it, "UTF-8") }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchWithNewPipe(videoId: String): String? {
        return try {
            Log.d("YT", "NewPipe: trying $videoId")
            val url = "https://www.youtube.com/watch?v=$videoId"
            val info = StreamInfo.getInfo(ServiceList.YouTube, url)
            val audioStreams = info.audioStreams
            if (audioStreams.isEmpty()) {
                Log.w("YT", "NewPipe: no audio streams for $videoId")
                return null
            }
            val best = audioStreams.maxByOrNull { it.averageBitrate }
            val audioUrl = best?.content
            if (audioUrl != null) {
                Log.d("YT", "NewPipe: success, host=${audioUrl.substringBefore('/').takeLast(40)}, bitrate=${best?.averageBitrate}")
            } else {
                Log.w("YT", "NewPipe: no URL in best stream")
            }
            audioUrl
        } catch (e: Exception) {
            Log.w("YT", "NewPipe failed for $videoId: ${e.message}")
            null
        }
    }

    private val pipedInstances = listOf(
        "pipedapi.kavin.rocks",
        "piped-api.garudalinux.org",
        "pipedapi.adminforge.de",
        "api.piped.yt",
        "pipedapi.coldforge.xyz",
        "api.piped.projectsegfau.lt"
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
        "inv.nadeko.net",
        "invidious.nerdvpn.de",
        "invidious.incogniweb.net",
        "iv.datura.network",
        "invidious.privacydev.net",
        "invidious.fdn.fr"
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
