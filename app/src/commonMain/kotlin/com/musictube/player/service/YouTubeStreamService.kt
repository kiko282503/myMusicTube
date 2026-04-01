package com.musictube.player.service

import com.musictube.player.platform.currentTimeMillis
import com.musictube.player.platform.logDebug
import com.musictube.player.platform.logError
import com.musictube.player.platform.logWarning
import com.musictube.player.service.esc
import com.musictube.player.service.jsonObjectOrNull
import com.musictube.player.service.str
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private const val TAG = "YouTubeStreamService"

class YouTubeStreamService {

    private data class CachedUrl(val url: String, val fetchedAt: Long = currentTimeMillis()) {
        fun isValid() = currentTimeMillis() - fetchedAt < 5 * 60 * 60 * 1000L
    }

    @Volatile private var urlCache: Map<String, CachedUrl> = emptyMap()
    private val urlCacheLock = Any()

    private val httpClient = HttpClient { engine { } }
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun extractAudioUrl(videoId: String): String? {
        urlCache[videoId]?.takeIf { it.isValid() }?.let {
            logDebug(TAG, "Cache hit for \$videoId")
            return it.url
        }
        logDebug(TAG, "Extracting audio for $videoId (parallel race)")
        val result = channelFlow {
            launch(Dispatchers.IO) { fetchWithYouTubeiAndroid(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithPiped(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithInvidious(videoId)?.let { send(it) } }
        }.firstOrNull()
        result?.let { url -> synchronized(urlCacheLock) { urlCache = urlCache + (videoId to CachedUrl(url)) } }
        return result
    }

    suspend fun prefetchAudioUrl(videoId: String) {
        if (urlCache[videoId]?.isValid() == true) return

        extractAudioUrl(videoId)
    }

    private suspend fun fetchWithYouTubeiAndroid(videoId: String): String? {
        return try {
            logDebug(TAG, "YouTubei Android: trying $videoId")
            val body = """
                {"context":{"client":{"clientName":"ANDROID","clientVersion":"19.17.34",
                "androidSdkVersion":34,"hl":"en","gl":"US"}},
                "videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}
            """.trimIndent()
            val raw = httpPost(
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false", body,
                mapOf(
                    "User-Agent"               to "com.google.android.youtube/19.17.34 (Linux; U; Android 14)",
                    "X-YouTube-Client-Name"    to "3",
                    "X-YouTube-Client-Version" to "19.17.34"
                )
            ) ?: return null
            extractAudioUrlFromPlayerJson(json.parseToJsonElement(raw).jsonObject)
                ?.also { logDebug(TAG, "YouTubei Android: success") }
        } catch (e: Exception) {
            logWarning(TAG, "YouTubei Android failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchWithPiped(videoId: String): String? {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.moomoo.me",
            "https://piped-api.garudalinux.org",
            "https://api.piped.projectsegfau.lt",
            "https://piped.video/api"
        )
        for (base in instances.shuffled()) {
            try {
                val raw = httpGet("$base/streams/$videoId") ?: continue
                val root = json.parseToJsonElement(raw).jsonObject
                val streams = root["audioStreams"]?.jsonArray ?: continue
                val best = streams.mapNotNull { it.jsonObjectOrNull }
                    .filter { it["mimeType"]?.str?.startsWith("audio/") == true }
                    .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                val url = best?.get("url")?.str ?: continue
                logDebug(TAG, "Piped success for $videoId via $base")
                return url
            } catch (_: Exception) { continue }
        }
        return null
    }

    private suspend fun fetchWithInvidious(videoId: String): String? {
        val instances = listOf(
            "https://invidious.snopyta.org",
            "https://invidious.kavin.rocks",
            "https://inv.riverside.rocks",
            "https://y.com.sb",
            "https://invidious.tiekoetter.com",
            "https://invidious.flokinet.to"
        )
        for (base in instances.shuffled()) {
            try {
                val raw = httpGet("$base/api/v1/videos/$videoId") ?: continue
                val root = json.parseToJsonElement(raw).jsonObject
                val streams = root["adaptiveFormats"]?.jsonArray ?: continue
                val best = streams.mapNotNull { it.jsonObjectOrNull }
                    .filter { it["type"]?.str?.startsWith("audio/") == true }
                    .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                val url = best?.get("url")?.str ?: continue
                logDebug(TAG, "Invidious success for $videoId")
                return url
            } catch (_: Exception) { continue }
        }
        return null
    }

    private fun extractAudioUrlFromPlayerJson(root: JsonObject): String? {
        val streaming = root["streamingData"]?.jsonObjectOrNull ?: return null
        val candidates = mutableListOf<JsonObject>()
        streaming["adaptiveFormats"]?.jsonArray?.forEach { candidates.add(it.jsonObject) }
        streaming["formats"]?.jsonArray?.forEach { candidates.add(it.jsonObject) }

        var bestUrl: String? = null
        var bestBitrate = -1
        for (fmt in candidates) {
            if (fmt["mimeType"]?.str?.startsWith("audio/") != true) continue
            val bitrate = fmt["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
            val url = fmt["url"]?.str ?: continue
            if (bitrate > bestBitrate) { bestBitrate = bitrate; bestUrl = url }
        }
        return bestUrl
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private suspend fun httpPost(url: String, body: String, extraHeaders: Map<String, String> = emptyMap()): String? {
        return try {
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                extraHeaders.forEach { (k, v) -> header(k, v) }
                setBody(body)
            }
            if (!response.status.isSuccess()) { logWarning(TAG, "HTTP ${response.status.value}"); return null }
            response.bodyAsText()
        } catch (e: Exception) { logError(TAG, "httpPost failed: ${e.message}", e); null }
    }

    private suspend fun httpGet(url: String): String? {
        return try {
            val response: HttpResponse = httpClient.get(url)
            if (!response.status.isSuccess()) return null
            response.bodyAsText()
        } catch (_: Exception) { null }
    }
}


