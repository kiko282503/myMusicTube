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
import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
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

    @Volatile private var cachedVisitorData: String = ""
    @Volatile private var visitorDataFetchedAt: Long = 0L

    // Shared OkHttpClient for all direct YouTube API calls.
    // Connection spec explicitly lists TLS 1.2 + 1.3 to work reliably on
    // vivo/OPPO/Xiaomi devices whose custom TLS stacks may omit 1.3 cipher suites.
    private val tlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(tlsSpec, ConnectionSpec.CLEARTEXT))
        .build()

    // Separate client for web-page scraping: YouTube HTML is 300-500 KB;
    // on slow mobile data (vivo/OPPO/Xiaomi budget phones) 10 s is too short.
    private val webPageClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .connectionSpecs(listOf(tlsSpec, ConnectionSpec.CLEARTEXT))
        .build()

    suspend fun extractAudioUrl(videoId: String): String? {
        urlCache[videoId]?.takeIf { it.isValid() }?.let {
            Log.d("YT", "Cache hit for $videoId")
            return it.url
        }
        Log.i("YT", "Extracting audio for: $videoId (parallel race)")
        val result = channelFlow {
            launch(Dispatchers.IO) { fetchWithMWeb(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithWebEmbedded(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithWebCreator(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithAndroid(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithAndroidTestSuite(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithIOS(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithAndroidMusic(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithTV(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithWebPage(videoId)?.let { send(it) } }
            launch(Dispatchers.IO) { fetchWithNewPipe(videoId)?.let { send(it) } }
        }.firstOrNull()
        if (result == null) {
            Log.e("YT", "All strategies failed for $videoId")
        } else {
            urlCache[videoId] = CachedUrl(result)
        }
        return result
    }

    /** Silently warms the cache for [videoId] without returning the URL. Safe to call in background. */
    suspend fun prefetchAudioUrl(videoId: String) {
        if (urlCache[videoId]?.isValid() == true) return
        extractAudioUrl(videoId)
    }

    /**
     * Fetches and caches YouTube visitor_data from the homepage.
     * Providing visitor_data helps bypass bot-detection for ANDROID/iOS innerTube clients.
     * Cached for 1 hour to avoid spamming the homepage endpoint.
     */
    private fun getVisitorData(): String {
        val now = System.currentTimeMillis()
        if (cachedVisitorData.isNotEmpty() && now - visitorDataFetchedAt < 60 * 60 * 1000L) {
            return cachedVisitorData
        }
        return try {
            val request = Request.Builder()
                .url("https://www.youtube.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11; SM-A515F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.88 Mobile Safari/537.36")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .build()
            val response = httpClient.newCall(request).execute()
            val html = response.body?.string() ?: return ""
            val match = Regex("\"VISITOR_DATA\"\\s*:\\s*\"([^\"]+)\"").find(html)
            val vd = match?.groupValues?.get(1) ?: ""
            if (vd.isNotEmpty()) {
                cachedVisitorData = vd
                visitorDataFetchedAt = now
                Log.d("YT", "Fetched visitorData: ${vd.take(20)}...")
            } else {
                Log.w("YT", "visitorData not found in YouTube homepage")
            }
            vd
        } catch (e: Exception) {
            Log.w("YT", "visitorData fetch failed: ${e.message}")
            ""
        }
    }

    /** ANDROID client v19.49.36 — returns direct stream URLs without JS signature requirement. */
    private fun fetchWithAndroid(videoId: String): String? {
        return try {
            Log.d("YT", "Android: trying $videoId")
            val ua = "com.google.android.youtube/19.49.36 (Linux; U; Android 11) gzip"
            val visitorData = getVisitorData()
            val clientExtra = if (visitorData.isNotEmpty()) """,\"visitorData\":\"$visitorData\"""" else ""
            val body = """{"context":{"client":{"clientName":"ANDROID","clientVersion":"19.49.36","androidSdkVersion":30,"userAgent":"$ua","hl":"en","gl":"US"$clientExtra}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", ua)
                .addHeader("X-YouTube-Client-Name", "3")
                .addHeader("X-YouTube-Client-Version", "19.49.36")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "Android HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val playabilityReason = root.getAsJsonObject("playabilityStatus")?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.d("YT", "Android: playability=$playability ($playabilityReason), hasStreamingData=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.d("YT", "Android: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
                if (adaptCount > 0) {
                    val first = sd.getAsJsonArray("adaptiveFormats")?.get(0)?.asJsonObject
                    Log.d("YT", "Android: first mime=${first?.get("mimeType")?.asString}, hasUrl=${first?.has("url")}, hasCipher=${first?.has("signatureCipher") == true || first?.has("cipher") == true}")
                }
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) {
                Log.d("YT", "Android: success")
                return directUrl
            }

            Log.w("YT", "Android: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "Android failed: ${e.message}")
            null
        }
    }

    /** iOS client — independent auth path from Android, good complement for redundancy. */
    private fun fetchWithIOS(videoId: String): String? {
        return try {
            Log.d("YT", "YouTubei IOS: trying $videoId")
            val ua = "com.google.ios.youtube/19.45.4 (iPhone16,2; U; CPU iPhone OS 17_5_1 like Mac OS X;) gzip"
            val body = """{
                "context": {
                    "client": {
                        "clientName": "IOS",
                        "clientVersion": "19.45.4",
                        "deviceMake": "Apple",
                        "deviceModel": "iPhone16,2",
                        "userAgent": "$ua",
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
                .addHeader("User-Agent", ua)
                .addHeader("X-YouTube-Client-Name", "5")
                .addHeader("X-YouTube-Client-Version", "19.45.4")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "YouTubei IOS HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val hasSD = root.has("streamingData")
            Log.d("YT", "YouTubei IOS: playability=$playability, hasStreamingData=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.d("YT", "YouTubei IOS: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) {
                Log.d("YT", "YouTubei IOS: success")
                return directUrl
            }

            Log.w("YT", "YouTubei IOS: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "YouTubei IOS failed: ${e.message}")
            null
        }
    }

    /** YouTube Music Android client — optimised for music videos, different auth path. */
    private fun fetchWithAndroidMusic(videoId: String): String? {
        return try {
            Log.d("YT", "AndroidMusic: trying $videoId")
            val ua = "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 11) gzip"
            val body = """{
                "context": {
                    "client": {
                        "clientName": "ANDROID_MUSIC",
                        "clientVersion": "7.27.52",
                        "androidSdkVersion": 30,
                        "userAgent": "$ua",
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
                .addHeader("User-Agent", ua)
                .addHeader("X-YouTube-Client-Name", "21")
                .addHeader("X-YouTube-Client-Version", "7.27.52")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "AndroidMusic HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val playabilityReason = root.getAsJsonObject("playabilityStatus")?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.d("YT", "AndroidMusic: playability=$playability ($playabilityReason), hasStreamingData=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.d("YT", "AndroidMusic: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
                if (adaptCount > 0) {
                    val first = sd.getAsJsonArray("adaptiveFormats")?.get(0)?.asJsonObject
                    Log.d("YT", "AndroidMusic: first mime=${first?.get("mimeType")?.asString}, hasUrl=${first?.has("url")}, hasCipher=${first?.has("signatureCipher") == true || first?.has("cipher") == true}")
                }
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) {
                Log.d("YT", "AndroidMusic: success")
                return directUrl
            }

            Log.w("YT", "AndroidMusic: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "AndroidMusic failed: ${e.message}")
            null
        }
    }

    /** ANDROID_TESTSUITE client — special internal client, returns HTTP 200; used for diagnostics. */
    private fun fetchWithAndroidTestSuite(videoId: String): String? {
        return try {
            Log.d("YT", "AndroidTestSuite: trying $videoId")
            val body = """{
                "context": {
                    "client": {
                        "clientName": "ANDROID_TESTSUITE",
                        "clientVersion": "1.9",
                        "androidSdkVersion": 30,
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
                .addHeader("User-Agent", "com.google.android.youtube/1.9 (Linux; U; Android 11) gzip")
                .addHeader("X-YouTube-Client-Name", "30")
                .addHeader("X-YouTube-Client-Version", "1.9")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "AndroidTestSuite HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playStatusObj = root.getAsJsonObject("playabilityStatus")
            val playability = playStatusObj?.get("status")?.asString
            val reason = playStatusObj?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.d("YT", "AndroidTestSuite: playability=$playability reason=$reason, hasStreamingData=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.d("YT", "AndroidTestSuite: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
                if (adaptCount > 0) {
                    val first = sd.getAsJsonArray("adaptiveFormats")?.get(0)?.asJsonObject
                    Log.d("YT", "AndroidTestSuite: first mime=${first?.get("mimeType")?.asString}, hasUrl=${first?.has("url")}, hasCipher=${first?.has("signatureCipher") == true || first?.has("cipher") == true}")
                }
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) {
                Log.d("YT", "AndroidTestSuite: success")
                return directUrl
            }

            Log.w("YT", "AndroidTestSuite: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "AndroidTestSuite failed: ${e.message}")
            null
        }
    }

    /** TVHTML5 Simply Embedded Player — TV clients often bypass age/embedding restrictions. */
    private fun fetchWithTV(videoId: String): String? {        return try {
            Log.d("YT", "TV: trying $videoId")
            val body = """{
                "context": {
                    "client": {
                        "clientName": "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
                        "clientVersion": "2.0",
                        "hl": "en",
                        "gl": "US"
                    },
                    "thirdParty": {
                        "embedUrl": "https://www.youtube.com"
                    }
                },
                "videoId": "$videoId",
                "contentCheckOk": true,
                "racyCheckOk": true
            }""".trimIndent()

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "Mozilla/5.0 (SMART-TV; LINUX; Tizen 6.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/6.0 TV Safari/538.1")
                .addHeader("X-YouTube-Client-Name", "85")
                .addHeader("X-YouTube-Client-Version", "2.0")
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "TV HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val hasSD = root.has("streamingData")
            Log.d("YT", "TV: playability=$playability, hasStreamingData=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.d("YT", "TV: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) {
                Log.d("YT", "TV: success")
                return directUrl
            }

            Log.w("YT", "TV: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "TV failed: ${e.message}")
            null
        }
    }

    private fun extractAudioUrlFromPlayerJson(root: com.google.gson.JsonObject): String? {
        val streamingData = root.getAsJsonObject("streamingData") ?: return null

        // HLS manifest — ExoPlayer handles HLS natively, good for live + some VOD
        streamingData.get("hlsManifestUrl")?.asString?.let { return it }

        // DASH manifest — ExoPlayer handles DASH natively (audio+video adaptive streams)
        streamingData.get("dashManifestUrl")?.asString?.let { return it }

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
            // Skip signatureCipher entries — the encrypted `s` param requires JS deobfuscation
            // (n-param transform) that we don't implement; passing the raw cipher URL to
            // ExoPlayer returns HTTP 403.  Clients that return cipher-only (e.g. WEB) are
            // deliberately skipped here so the channelFlow race picks a better strategy.
            val url = direct ?: continue
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

    /** Fetch the YouTube watch page HTML and extract ytInitialPlayerResponse as fallback.
     *  Uses a dedicated client with a 30 s read timeout — the HTML response can be
     *  300-500 KB which exceeds the 10 s budget on slow mobile data typical of
     *  budget vivo/OPPO/Xiaomi devices. */
    private fun fetchWithWebPage(videoId: String): String? {
        return try {
            Log.d("YT", "WebPage: trying $videoId")
            val request = Request.Builder()
                .url("https://www.youtube.com/watch?v=$videoId&bpctr=9999999999&has_verified=1&hl=en")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11; vivo 1901) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Encoding", "identity")
                .build()

            val response = webPageClient.newCall(request).execute()
            val html = response.body?.string()
            if (!response.isSuccessful || html == null) {
                Log.w("YT", "WebPage HTTP ${response.code}")
                return null
            }

            val marker = "ytInitialPlayerResponse = "
            val idx = html.indexOf(marker)
            if (idx < 0) {
                Log.w("YT", "WebPage: no ytInitialPlayerResponse in HTML")
                return null
            }

            val jsonStr = extractJsonObject(html, idx + marker.length)
            if (jsonStr == null) {
                // ytInitialPlayerResponse = null (page returned a null/non-object response)
                Log.i("YT", "WebPage: ytInitialPlayerResponse is null/non-object (len=${html.length})")
                return null
            }

            val root = JsonParser.parseString(jsonStr).asJsonObject
            val statusObj = root.getAsJsonObject("playabilityStatus")
            val status = statusObj?.get("status")?.asString
            val reason = statusObj?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.d("YT", "WebPage: status=$status reason=$reason hasSD=$hasSD htmlLen=${html.length}")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                val hasHls = sd.has("hlsManifestUrl")
                val hasDash = sd.has("dashManifestUrl")
                Log.d("YT", "WebPage: adaptiveFormats=$adaptCount hasHls=$hasHls hasDash=$hasDash")
                if (adaptCount > 0) {
                    val first = sd.getAsJsonArray("adaptiveFormats")?.get(0)?.asJsonObject
                    Log.d("YT", "WebPage: first mime=${first?.get("mimeType")?.asString} hasUrl=${first?.has("url")} hasCipher=${first?.has("signatureCipher")}")
                }
            }

            val url = extractAudioUrlFromPlayerJson(root)
            if (url != null) {
                Log.d("YT", "WebPage: success")
                return url
            }

            Log.w("YT", "WebPage: no usable audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "WebPage failed: ${e.message}")
            null
        }
    }

    private fun extractJsonObject(str: String, start: Int): String? {
        var depth = 0
        var inStr = false
        var escaped = false
        val sb = StringBuilder()
        for (i in start until str.length) {
            val c = str[i]
            // Skip leading whitespace before the opening brace
            if (depth == 0) {
                when (c) {
                    ' ', '\t', '\n', '\r' -> continue  // skip whitespace
                    '{' -> { depth = 1; sb.append(c) }   // start of JSON object
                    else -> return null                   // not a JSON object (e.g., null, string)
                }
                continue
            }
            sb.append(c)
            if (escaped) { escaped = false; continue }
            if (c == '\\' && inStr) { escaped = true; continue }
            if (c == '"') { inStr = !inStr; continue }
            if (!inStr) {
                when (c) {
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) return sb.toString() }
                }
            }
        }
        return null
    }

    private fun fetchWithNewPipe(videoId: String): String? {
        return try {
            Log.i("YT", "NewPipe: trying $videoId")
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
                Log.i("YT", "NewPipe: success bitrate=${best?.averageBitrate}")
            } else {
                Log.w("YT", "NewPipe: no URL in best stream")
            }
            audioUrl
        } catch (e: Throwable) {
            Log.w("YT", "NewPipe failed: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
            var cause = e.cause
            var depth = 0
            while (cause != null && depth < 3) {
                Log.w("YT", "  cause: ${cause.javaClass.simpleName}: ${cause.message?.take(100)}")
                cause = cause.cause
                depth++
            }
            null
        }
    }

    /**
     * MWEB (mobile web) client — widely reported to return direct (non-cipher) audio URLs
     * without requiring po_token as of 2025.  First in the race because it has the highest
     * success rate on unauthed requests.
     */
    private fun fetchWithMWeb(videoId: String): String? {
        return try {
            Log.i("YT", "MWEB: trying $videoId")
            val ua = "Mozilla/5.0 (Linux; Android 11; Pixel 3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.88 Mobile Safari/537.36"
            val body = """{"context":{"client":{"clientName":"MWEB","clientVersion":"2.20231219.04.00","userAgent":"$ua","hl":"en","gl":"US"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", ua)
                .addHeader("X-YouTube-Client-Name", "2")
                .addHeader("X-YouTube-Client-Version", "2.20231219.04.00")
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("Referer", "https://www.youtube.com/")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "MWEB HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val playabilityReason = root.getAsJsonObject("playabilityStatus")?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.i("YT", "MWEB: playability=$playability ($playabilityReason), hasSD=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.i("YT", "MWEB: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) { Log.i("YT", "MWEB: success"); return directUrl }
            Log.w("YT", "MWEB: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "MWEB failed: ${e.message}")
            null
        }
    }

    /**
     * WEB_EMBEDDED_PLAYER (client 56) — YouTube's iframe embed client.
     * Embedded-player requests travel through a different bot-detection pipeline
     * than first-party clients; frequently returns streaming data without po_token.
     */
    private fun fetchWithWebEmbedded(videoId: String): String? {
        return try {
            Log.i("YT", "WebEmbedded: trying $videoId")
            val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            val body = """{"context":{"client":{"clientName":"WEB_EMBEDDED_PLAYER","clientVersion":"2.20240401.13.00","userAgent":"$ua","hl":"en","gl":"US"},"thirdParty":{"embedUrl":"https://www.youtube.com/","embedDomain":"www.youtube.com"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", ua)
                .addHeader("X-YouTube-Client-Name", "56")
                .addHeader("X-YouTube-Client-Version", "2.20240401.13.00")
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", "https://www.youtube.com")
                .addHeader("Referer", "https://www.youtube.com/")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "WebEmbedded HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val playabilityReason = root.getAsJsonObject("playabilityStatus")?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.i("YT", "WebEmbedded: playability=$playability ($playabilityReason), hasSD=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.i("YT", "WebEmbedded: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) { Log.i("YT", "WebEmbedded: success"); return directUrl }
            Log.w("YT", "WebEmbedded: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "WebEmbedded failed: ${e.message}")
            null
        }
    }

    /**
     * WEB_CREATOR (YouTube Studio web, client 62) — a first-party creator client
     * that's exempt from some of the stricter bot-detection applied to viewer clients.
     */
    private fun fetchWithWebCreator(videoId: String): String? {
        return try {
            Log.i("YT", "WebCreator: trying $videoId")
            val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            val body = """{"context":{"client":{"clientName":"WEB_CREATOR","clientVersion":"1.20240228.07.00","userAgent":"$ua","hl":"en","gl":"US"}},"videoId":"$videoId","contentCheckOk":true,"racyCheckOk":true}"""

            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", ua)
                .addHeader("X-YouTube-Client-Name", "62")
                .addHeader("X-YouTube-Client-Version", "1.20240228.07.00")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val raw = response.body?.string()
            if (!response.isSuccessful || raw == null) {
                Log.w("YT", "WebCreator HTTP ${response.code}")
                return null
            }

            val root = JsonParser.parseString(raw).asJsonObject
            val playability = root.getAsJsonObject("playabilityStatus")?.get("status")?.asString
            val playabilityReason = root.getAsJsonObject("playabilityStatus")?.get("reason")?.asString
            val hasSD = root.has("streamingData")
            Log.i("YT", "WebCreator: playability=$playability ($playabilityReason), hasSD=$hasSD")
            if (hasSD) {
                val sd = root.getAsJsonObject("streamingData")
                val adaptCount = sd.getAsJsonArray("adaptiveFormats")?.size() ?: 0
                Log.i("YT", "WebCreator: adaptiveFormats=$adaptCount, hasHls=${sd.has("hlsManifestUrl")}")
            }

            val directUrl = extractAudioUrlFromPlayerJson(root)
            if (directUrl != null) { Log.i("YT", "WebCreator: success"); return directUrl }
            Log.w("YT", "WebCreator: no direct audio URL")
            null
        } catch (e: Exception) {
            Log.w("YT", "WebCreator failed: ${e.message}")
            null
        }
    }
}

