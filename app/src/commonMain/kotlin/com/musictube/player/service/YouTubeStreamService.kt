package com.musictube.player.service

import com.musictube.player.platform.currentTimeMillis
import com.musictube.player.platform.extractYouTubeAudioViaNewPipe
import com.musictube.player.platform.logDebug
import com.musictube.player.platform.logError
import com.musictube.player.platform.logWarning
import com.musictube.player.service.esc
import com.musictube.player.service.jsonObjectOrNull
import com.musictube.player.service.str
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

private const val TAG = "YouTubeStreamService"
private const val VISITOR_TOKEN_TTL = 30 * 60 * 1000L

class YouTubeStreamService {

    private data class CachedUrl(val url: String, val fetchedAt: Long = currentTimeMillis()) {
        fun isValid() = currentTimeMillis() - fetchedAt < 5 * 60 * 60 * 1000L
    }

    @kotlin.concurrent.Volatile private var urlCache: Map<String, CachedUrl> = emptyMap()
    private val urlCacheMutex = Mutex()

    @kotlin.concurrent.Volatile private var visitorToken: String? = null
    @kotlin.concurrent.Volatile private var visitorTokenTime: Long = 0L

    private val httpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000
            requestTimeoutMillis = 10_000
            socketTimeoutMillis = 8_000
        }
    }
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun getOrFetchVisitorToken(): String? {
        val current = visitorToken
        if (current != null && currentTimeMillis() - visitorTokenTime < VISITOR_TOKEN_TTL) return current
        return try {
            val response: HttpResponse = httpClient.get("https://www.youtube.com/") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                header("Accept-Language", "en-US,en;q=0.9")
            }
            val html = response.bodyAsText()
            val regex = Regex(""""VISITOR_INFO1_LIVE":"([^"]+)"""")
            regex.find(html)?.groupValues?.getOrNull(1)?.also {
                visitorToken = it
                visitorTokenTime = currentTimeMillis()
                logDebug(TAG, "Fetched visitor token: ${it.take(8)}...")
            }
        } catch (_: Exception) { null }
    }

    suspend fun extractAudioUrl(videoId: String): String? {
        urlCache[videoId]?.takeIf { it.isValid() }?.let {
            logDebug(TAG, "Cache hit for \$videoId")
            return it.url
        }
        logDebug(TAG, "Extracting audio for $videoId (parallel race)")
        val result = withTimeoutOrNull(25_000) {
            channelFlow {
                launch(Dispatchers.IO) { extractYouTubeAudioViaNewPipe(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithYouTubeiIos(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithYouTubeiTv(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithAndroidMusic(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithYouTubeiAndroid(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithAndroidTestSuite(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithPiped(videoId)?.let { send(it) } }
                launch(Dispatchers.IO) { fetchWithInvidious(videoId)?.let { send(it) } }
            }.firstOrNull()
        }
        result?.let { url -> urlCacheMutex.withLock { urlCache = urlCache + (videoId to CachedUrl(url)) } }
        return result
    }

    suspend fun prefetchAudioUrl(videoId: String) {
        if (urlCache[videoId]?.isValid() == true) return
        extractAudioUrl(videoId)
    }

    private suspend fun fetchWithYouTubeiIos(videoId: String): String? {
        return try {
            logDebug(TAG, "YouTubei iOS: trying $videoId")
            val ua = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X);gzip"
            val token = getOrFetchVisitorToken()
            val body = """{"context":{"client":{"clientName":"IOS","clientVersion":"19.29.1","deviceMake":"Apple","deviceModel":"iPhone16,2","userAgent":"$ua","osName":"iPhone","osVersion":"17.5.1.21F90","hl":"en","gl":"US"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""
            val headers = buildMap<String, String> {
                put("User-Agent", ua)
                put("X-YouTube-Client-Name", "5")
                put("X-YouTube-Client-Version", "19.29.1")
                if (token != null) put("Cookie", "VISITOR_INFO1_LIVE=$token")
            }
            val raw = httpPost(
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false", body, headers
            ) ?: return null
            extractAudioUrlFromPlayerJson(json.parseToJsonElement(raw).jsonObject, "iOS")
                ?.also { logDebug(TAG, "YouTubei iOS: success") }
        } catch (e: Exception) {
            logWarning(TAG, "YouTubei iOS failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchWithYouTubeiTv(videoId: String): String? {
        return try {
            logDebug(TAG, "YouTubei TV: trying $videoId")
            val body = """{"context":{"client":{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","hl":"en","gl":"US"},"thirdParty":{"embedUrl":"https://www.youtube.com/"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""
            val raw = httpPost(
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false", body,
                mapOf(
                    "User-Agent"               to "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/538.1",
                    "X-YouTube-Client-Name"    to "85",
                    "X-YouTube-Client-Version" to "2.0",
                    "Origin"                   to "https://www.youtube.com",
                    "Referer"                  to "https://www.youtube.com/"
                )
            ) ?: return null
            extractAudioUrlFromPlayerJson(json.parseToJsonElement(raw).jsonObject, "TV")
                ?.also { logDebug(TAG, "YouTubei TV: success") }
        } catch (e: Exception) {
            logWarning(TAG, "YouTubei TV failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchWithAndroidMusic(videoId: String): String? {
        return try {
            logDebug(TAG, "YouTubei AndroidMusic: trying $videoId")
            val body = """{"context":{"client":{"clientName":"ANDROID_MUSIC","clientVersion":"6.42.52","androidSdkVersion":30,"hl":"en","gl":"US"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""
            val raw = httpPost(
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false", body,
                mapOf(
                    "User-Agent"               to "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 11) gzip",
                    "X-YouTube-Client-Name"    to "21",
                    "X-YouTube-Client-Version" to "6.42.52"
                )
            ) ?: return null
            extractAudioUrlFromPlayerJson(json.parseToJsonElement(raw).jsonObject, "AndroidMusic")
                ?.also { logDebug(TAG, "YouTubei AndroidMusic: success") }
        } catch (e: Exception) {
            logWarning(TAG, "YouTubei AndroidMusic failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchWithYouTubeiAndroid(videoId: String): String? {
        return try {
            logDebug(TAG, "YouTubei Android: trying $videoId")
            val token = getOrFetchVisitorToken()
            val body = """{"context":{"client":{"clientName":"ANDROID","clientVersion":"19.17.34","androidSdkVersion":34,"hl":"en","gl":"US"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""
            val headers = buildMap<String, String> {
                put("User-Agent", "com.google.android.youtube/19.17.34 (Linux; U; Android 14)")
                put("X-YouTube-Client-Name", "3")
                put("X-YouTube-Client-Version", "19.17.34")
                if (token != null) put("Cookie", "VISITOR_INFO1_LIVE=$token")
            }
            val raw = httpPost(
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false", body, headers
            ) ?: return null
            extractAudioUrlFromPlayerJson(json.parseToJsonElement(raw).jsonObject, "Android")
                ?.also { logDebug(TAG, "YouTubei Android: success") }
        } catch (e: Exception) {
            logWarning(TAG, "YouTubei Android failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchWithAndroidTestSuite(videoId: String): String? {
        return try {
            logDebug(TAG, "YouTubei AndroidTestSuite: trying $videoId")
            val body = """{"context":{"client":{"clientName":"ANDROID_TESTSUITE","clientVersion":"1.9","androidSdkVersion":30,"platform":"MOBILE","hl":"en","gl":"US"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""
            val raw = httpPost(
                "https://www.youtube.com/youtubei/v1/player?prettyPrint=false", body,
                mapOf(
                    "User-Agent"               to "com.google.android.youtube/1.9 (Linux; U; Android 11) gzip",
                    "X-YouTube-Client-Name"    to "30",
                    "X-YouTube-Client-Version" to "1.9"
                )
            ) ?: return null
            extractAudioUrlFromPlayerJson(json.parseToJsonElement(raw).jsonObject, "AndroidTestSuite")
                ?.also { logDebug(TAG, "YouTubei AndroidTestSuite: success") }
        } catch (e: Exception) {
            logWarning(TAG, "YouTubei AndroidTestSuite failed: ${e.message}")
            null
        }
    }

    private suspend fun fetchWithPiped(videoId: String): String? {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://watchapi.whatever.social",
            "https://api.piped.privacydev.net",
            "https://pipedapi.tokhmi.xyz",
            "https://piped-api.garudalinux.org",
            "https://piped.video/api"
        )
        // Run all instances in parallel for speed; return first success
        return withTimeoutOrNull(9_000) {
            channelFlow {
                for (base in instances.shuffled()) {
                    launch(Dispatchers.IO) {
                        try {
                            val raw = httpGet("$base/streams/$videoId") ?: return@launch
                            val root = json.parseToJsonElement(raw).jsonObject
                            val streams = root["audioStreams"]?.jsonArray ?: return@launch
                            val best = streams.mapNotNull { it.jsonObjectOrNull }
                                .filter { it["mimeType"]?.str?.startsWith("audio/") == true }
                                .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                            val url = best?.get("url")?.str ?: return@launch
                            logDebug(TAG, "Piped success for $videoId via $base")
                            send(url)
                        } catch (_: Exception) {}
                    }
                }
            }.firstOrNull()
        }
    }

    private suspend fun fetchWithInvidious(videoId: String): String? {
        val instances = listOf(
            "https://inv.nadeko.net",
            "https://invidious.privacydev.net",
            "https://invidious.kavin.rocks",
            "https://inv.riverside.rocks",
            "https://invidious.tiekoetter.com",
            "https://invidious.flokinet.to"
        )
        return withTimeoutOrNull(9_000) {
            channelFlow {
                for (base in instances.shuffled()) {
                    launch(Dispatchers.IO) {
                        try {
                            val raw = httpGet("$base/api/v1/videos/$videoId") ?: return@launch
                            val root = json.parseToJsonElement(raw).jsonObject
                            val streams = root["adaptiveFormats"]?.jsonArray ?: return@launch
                            val best = streams.mapNotNull { it.jsonObjectOrNull }
                                .filter { it["type"]?.str?.startsWith("audio/") == true }
                                .maxByOrNull { it["bitrate"]?.jsonPrimitive?.intOrNull ?: 0 }
                            val url = best?.get("url")?.str ?: return@launch
                            logDebug(TAG, "Invidious success for $videoId via $base")
                            send(url)
                        } catch (_: Exception) {}
                    }
                }
            }.firstOrNull()
        }
    }

    private fun extractAudioUrlFromPlayerJson(root: JsonObject, client: String = ""): String? {
        val playabilityStatus = root["playabilityStatus"]?.jsonObjectOrNull?.get("status")?.str
        if (playabilityStatus != null && playabilityStatus != "OK") {
            logWarning(TAG, "[$client] Video not playable: $playabilityStatus")
            return null
        }
        val streaming = root["streamingData"]?.jsonObjectOrNull ?: run {
            logWarning(TAG, "[$client] No streamingData in response")
            return null
        }
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
        if (bestUrl == null) logWarning(TAG, "[$client] No audio URL found in streamingData")
        return bestUrl
    }

    // â”€â”€ HTTP helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

