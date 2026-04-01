package com.musictube.player.service

import com.musictube.player.data.model.SearchResult
import com.musictube.player.platform.logDebug
import com.musictube.player.platform.logError
import com.musictube.player.platform.logWarning
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private const val TAG = "SearchService"

class SearchService {

    internal val httpClient = HttpClient {
        engine { }
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val baseContext = """{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}}"""
    private val searchUrl = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private val browseUrl  = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"

    private val ytHeaders: Map<String, String> = mapOf(
        "User-Agent"               to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "X-YouTube-Client-Name"    to "67",
        "X-YouTube-Client-Version" to "1.20240101.01.00",
        "Origin"                   to "https://music.youtube.com",
        "Referer"                  to "https://music.youtube.com/"
    )

    suspend fun searchMusic(query: String, songsOnly: Boolean = false, maxPages: Int = 1): List<SearchResult> =
        withContext(Dispatchers.IO) {
            try { searchYouTube(query, songsOnly, maxPages) }
            catch (e: Exception) { logError(TAG, "search failed: ${e.message}", e); emptyList() }
        }

    suspend fun searchMusicPaged(query: String, songsOnly: Boolean = false): Pair<List<SearchResult>, String?> =
        withContext(Dispatchers.IO) {
            try {
                val body = buildRequestBody(query, songsOnly)
                val raw  = postJson(searchUrl, body) ?: return@withContext Pair(emptyList(), null)
                val root = json.parseToJsonElement(raw).jsonObject
                val collected = mutableListOf<SearchResult>()
                findMusicListItemRenderers(root, collected, songsOnly)
                val token = findFirstContinuationToken(root)
                Pair(collected.distinctBy { it.id }, token)
            } catch (e: Exception) {
                logError(TAG, "searchMusicPaged failed: ${e.message}", e)
                Pair(emptyList(), null)
            }
        }

    suspend fun fetchContinuation(continuationToken: String, songsOnly: Boolean = false): Pair<List<SearchResult>, String?> =
        withContext(Dispatchers.IO) {
            try {
                val safe = continuationToken.esc()
                val body = """{"context":$baseContext,"continuation":"$safe"}"""
                val raw  = postJson(searchUrl, body) ?: return@withContext Pair(emptyList(), null)
                val root = json.parseToJsonElement(raw).jsonObject
                val collected = mutableListOf<SearchResult>()
                findMusicListItemRenderers(root, collected, songsOnly)
                val next = findFirstContinuationToken(root)
                val resolved = if (next == continuationToken) null else next
                Pair(collected.distinctBy { it.id }, resolved)
            } catch (e: Exception) {
                logError(TAG, "fetchContinuation failed: ${e.message}", e)
                Pair(emptyList(), null)
            }
        }

    private suspend fun searchYouTube(query: String, songsOnly: Boolean, maxPages: Int): List<SearchResult> {
        val body  = buildRequestBody(query, songsOnly)
        val collected = mutableListOf<SearchResult>()
        val seenTokens = mutableSetOf<String>()

        val firstRaw = postJson(searchUrl, body) ?: return emptyList()
        val firstRoot = json.parseToJsonElement(firstRaw).jsonObject
        findMusicListItemRenderers(firstRoot, collected, songsOnly)
        var token = findFirstContinuationToken(firstRoot)
        token?.let { seenTokens.add(it) }

        var page = 1
        while (token != null && page < maxPages && collected.size < maxPages * 25) {
            val contBody = """{"context":$baseContext,"continuation":"${token.esc()}"}"""
            val contRaw  = postJson(searchUrl, contBody) ?: break
            val contRoot = json.parseToJsonElement(contRaw).jsonObject
            findMusicListItemRenderers(contRoot, collected, songsOnly)
            val next = findFirstContinuationToken(contRoot)
            token = if (next == null || !seenTokens.add(next)) null else next
            page++
        }
        return collected.distinctBy { it.id }.take(maxPages * 25)
    }

    suspend fun fetchHomeFeed(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val body = """{"context":$baseContext,"browseId":"FEmusic_home"}"""
            val raw  = postJson(browseUrl, body) ?: return@withContext emptyList()
            val root = json.parseToJsonElement(raw).jsonObject
            val collected = mutableListOf<SearchResult>()
            findMusicListItemRenderers(root, collected, songsOnly = false)
            findMusicTwoRowItemRenderers(root, collected)
            collected.filter { it.isPlayable }.distinctBy { it.id }
        } catch (e: Exception) {
            logError(TAG, "fetchHomeFeed failed: ${e.message}", e)
            emptyList()
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────────────────

    private suspend fun postJson(url: String, body: String): String? {
        return try {
            val response: HttpResponse = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                ytHeaders.forEach { (k, v) -> header(k, v) }
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                logWarning(TAG, "HTTP ${response.status.value} from $url")
                return null
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logError(TAG, "postJson error: ${e.message}", e)
            null
        }
    }

    // ── JSON tree walking ─────────────────────────────────────────────────────

    private fun findMusicTwoRowItemRenderers(element: JsonElement, results: MutableList<SearchResult>) {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("musicTwoRowItemRenderer")) {
                    parseMusicTwoRowItemRenderer(element["musicTwoRowItemRenderer"]!!.jsonObject)
                        ?.let { results.add(it) }
                } else element.values.forEach { findMusicTwoRowItemRenderers(it, results) }
            }
            is JsonArray  -> element.forEach { findMusicTwoRowItemRenderers(it, results) }
            else          -> {}
        }
    }

    private fun parseMusicTwoRowItemRenderer(r: JsonObject): SearchResult? {
        return try {
            val videoId = r["navigationEndpoint"]?.jsonObjectOrNull
                ?.get("watchEndpoint")?.jsonObjectOrNull
                ?.get("videoId")?.str
                ?: r["overlay"]?.jsonObjectOrNull
                    ?.get("musicItemThumbnailOverlayRenderer")?.jsonObjectOrNull
                    ?.get("content")?.jsonObjectOrNull
                    ?.get("musicPlayButtonRenderer")?.jsonObjectOrNull
                    ?.get("playNavigationEndpoint")?.jsonObjectOrNull
                    ?.get("watchEndpoint")?.jsonObjectOrNull
                    ?.get("videoId")?.str
            if (videoId.isNullOrBlank()) return null

            val title = r["title"]?.jsonObjectOrNull
                ?.get("runs")?.jsonArrayOrNull
                ?.firstOrNull()?.jsonObjectOrNull
                ?.get("text")?.str ?: return null

            val typeLabels = setOf("song","single","ep","album","artist","playlist","podcast","episode","video")
            val artist = r["subtitle"]?.jsonObjectOrNull
                ?.get("runs")?.jsonArrayOrNull
                ?.mapNotNull { it.jsonObjectOrNull?.get("text")?.str }
                ?.firstOrNull { t -> t.trim().isNotEmpty() && t.trim() != "•" && t.trim().lowercase() !in typeLabels }
                ?: ""

            val thumbnail = r["thumbnailRenderer"]?.jsonObjectOrNull
                ?.get("musicThumbnailRenderer")?.jsonObjectOrNull
                ?.get("thumbnail")?.jsonObjectOrNull
                ?.get("thumbnails")?.jsonArrayOrNull
                ?.lastOrNull()?.jsonObjectOrNull
                ?.get("url")?.str
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            SearchResult(videoId, title, artist, "", thumbnail,
                "https://music.youtube.com/watch?v=$videoId", null, "song", true)
        } catch (_: Exception) { null }
    }

    private fun findMusicListItemRenderers(element: JsonElement, results: MutableList<SearchResult>, songsOnly: Boolean) {
        when (element) {
            is JsonObject -> {
                if (element.containsKey("musicResponsiveListItemRenderer")) {
                    parseMusicListItemRenderer(
                        element["musicResponsiveListItemRenderer"]!!.jsonObject, songsOnly
                    )?.let { results.add(it) }
                } else element.values.forEach { findMusicListItemRenderers(it, results, songsOnly) }
            }
            is JsonArray  -> element.forEach { findMusicListItemRenderers(it, results, songsOnly) }
            else          -> {}
        }
    }

    private fun parseMusicListItemRenderer(r: JsonObject, songsOnly: Boolean): SearchResult? {
        return try {
            val flexCols = r["flexColumns"]?.jsonArrayOrNull
            val typeText = flexCols?.getOrNull(1)?.jsonObjectOrNull
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObjectOrNull
                ?.get("text")?.jsonObjectOrNull
                ?.get("runs")?.jsonArrayOrNull
                ?.firstOrNull()?.jsonObjectOrNull
                ?.get("text")?.str?.trim()?.lowercase() ?: ""

            if (!songsOnly && typeText in setOf("playlist","album","artist","podcast","episode")) return null

            val videoId = r["overlay"]?.jsonObjectOrNull
                ?.get("musicItemThumbnailOverlayRenderer")?.jsonObjectOrNull
                ?.get("content")?.jsonObjectOrNull
                ?.get("musicPlayButtonRenderer")?.jsonObjectOrNull
                ?.get("playNavigationEndpoint")?.jsonObjectOrNull
                ?.get("watchEndpoint")?.jsonObjectOrNull
                ?.get("videoId")?.str
                ?: r["flexColumns"]?.jsonArrayOrNull
                    ?.firstOrNull()?.jsonObjectOrNull
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObjectOrNull
                    ?.get("text")?.jsonObjectOrNull
                    ?.get("runs")?.jsonArrayOrNull
                    ?.firstOrNull()?.jsonObjectOrNull
                    ?.get("navigationEndpoint")?.jsonObjectOrNull
                    ?.get("watchEndpoint")?.jsonObjectOrNull
                    ?.get("videoId")?.str

            val title = flexCols?.firstOrNull()?.jsonObjectOrNull
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObjectOrNull
                ?.get("text")?.jsonObjectOrNull
                ?.get("runs")?.jsonArrayOrNull
                ?.firstOrNull()?.jsonObjectOrNull
                ?.get("text")?.str ?: return null

            val typeLabels = setOf("song","single","ep","album","artist","playlist","podcast","episode","video")
            val subtitleRuns = r["flexColumns"]?.jsonArrayOrNull
                ?.getOrNull(1)?.jsonObjectOrNull
                ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObjectOrNull
                ?.get("text")?.jsonObjectOrNull
                ?.get("runs")?.jsonArrayOrNull
            val artist = subtitleRuns
                ?.mapNotNull { it.jsonObjectOrNull?.get("text")?.str }
                ?.firstOrNull { t -> t.trim().isNotEmpty() && t.trim() != "•" && t.trim().lowercase() !in typeLabels }
                ?: ""

            val thumbnail = r["thumbnail"]?.jsonObjectOrNull
                ?.get("musicThumbnailRenderer")?.jsonObjectOrNull
                ?.get("thumbnail")?.jsonObjectOrNull
                ?.get("thumbnails")?.jsonArrayOrNull
                ?.lastOrNull()?.jsonObjectOrNull
                ?.get("url")?.str
                ?: videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" } ?: ""

            SearchResult(
                id = videoId ?: return null,
                title = title, artist = artist, duration = "",
                thumbnailUrl = thumbnail,
                videoUrl = "https://music.youtube.com/watch?v=$videoId",
                audioUrl = null, itemType = "song",
                isPlayable = !videoId.isNullOrBlank()
            )
        } catch (_: Exception) { null }
    }

    private fun findFirstContinuationToken(root: JsonObject): String? {
        // Structured path — initial songsOnly search
        val fromSearch = root["contents"]?.jsonObjectOrNull
            ?.get("tabbedSearchResultsRenderer")?.jsonObjectOrNull
            ?.get("tabs")?.jsonArrayOrNull?.firstOrNull()?.jsonObjectOrNull
            ?.get("tabRenderer")?.jsonObjectOrNull
            ?.get("content")?.jsonObjectOrNull
            ?.get("sectionListRenderer")?.jsonObjectOrNull
            ?.get("contents")?.jsonArrayOrNull
            ?.firstOrNull { it.jsonObjectOrNull?.containsKey("musicShelfRenderer") == true }
            ?.jsonObjectOrNull?.get("musicShelfRenderer")?.jsonObjectOrNull
            ?.get("continuations")?.jsonArrayOrNull
            ?.firstOrNull()?.jsonObjectOrNull
            ?.deepGetToken()
        if (fromSearch != null) return fromSearch

        // Structured path — continuation response
        val fromCont = root["continuationContents"]?.jsonObjectOrNull
            ?.get("musicShelfContinuation")?.jsonObjectOrNull
            ?.get("continuations")?.jsonArrayOrNull
            ?.firstOrNull()?.jsonObjectOrNull
            ?.deepGetToken()
        if (fromCont != null) return fromCont

        return fallbackScan(root)
    }

    private fun JsonObject.deepGetToken(): String? {
        val t1 = get("nextContinuationData")?.jsonObjectOrNull?.get("continuation")?.str?.takeIf { it.length > 50 }
        if (t1 != null) return t1
        return get("reloadContinuationData")?.jsonObjectOrNull?.get("continuation")?.str?.takeIf { it.length > 50 }
    }

    private fun fallbackScan(element: JsonElement): String? {
        return when (element) {
            is JsonObject -> {
                val direct = element["continuation"]?.str?.takeIf { it.length > 50 }
                if (direct != null) return direct
                for ((key, value) in element) {
                    if (key in SKIP_KEYS) continue
                    val t = fallbackScan(value)
                    if (t != null) return t
                }
                null
            }
            is JsonArray  -> element.firstNotNullOfOrNull { fallbackScan(it) }
            else          -> null
        }
    }

    private fun buildRequestBody(query: String, songsOnly: Boolean): String {
        val safeQ = query.esc()
        val params = if (songsOnly) ""","params":"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"""" else ""
        return """{"context":$baseContext,"query":"$safeQ"$params}"""
    }

    companion object {
        private val SKIP_KEYS = setOf(
            "musicResponsiveListItemRenderer", "musicTwoRowItemRenderer",
            "navigationEndpoint", "continuationEndpoint", "watchEndpoint",
            "browseEndpoint", "overlay", "flexColumns", "fixedColumns", "thumbnail"
        )
    }
}

// ── JsonElement helpers ───────────────────────────────────────────────────────

internal val JsonElement.str: String? get() = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
internal val JsonElement.jsonObjectOrNull: JsonObject? get() = this as? JsonObject
internal val JsonElement.jsonArrayOrNull: JsonArray? get() = this as? JsonArray
internal fun String.esc() = replace("\\", "\\\\").replace("\"", "\\\"")
