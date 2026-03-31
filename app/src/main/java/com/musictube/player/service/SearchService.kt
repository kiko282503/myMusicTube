package com.musictube.player.service

import android.util.Log
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.musictube.player.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchService @Inject constructor() {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val baseContextJson = """{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}}"""
    private val searchUrl = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
    private val browseUrl = "https://music.youtube.com/youtubei/v1/browse?prettyPrint=false"

    // songsOnly=true adds the YouTube Music "Songs" filter so only individual tracks are returned
    // maxPages controls how many continuation pages are fetched (1 = first page only, for fast initial load)
    suspend fun searchMusic(query: String, songsOnly: Boolean = false, maxPages: Int = 1): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            searchYouTube(query, songsOnly, maxPages)
        } catch (e: Exception) {
            Log.e("SearchService", "YouTube search failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Returns the first page of results AND the continuation token for the next page.
     * Use [fetchContinuation] to load subsequent pages.
     */
    suspend fun searchMusicPaged(query: String, songsOnly: Boolean = false): Pair<List<SearchResult>, String?> = withContext(Dispatchers.IO) {
        try {
            val safeQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
            val paramsField = if (songsOnly) ",\"params\":\"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D\"" else ""
            val body = """{"context":$baseContextJson,"query":"$safeQuery"$paramsField}"""
            val json = executeSearchRequest(body) ?: return@withContext Pair(emptyList(), null)
            val root = JsonParser.parseString(json).asJsonObject
            val collected = mutableListOf<SearchResult>()
            findMusicListItemRenderers(root, collected, songsOnly)
            val token = findFirstContinuationToken(root)
            Pair(collected.distinctBy { it.id }, token)
        } catch (e: Exception) {
            Log.e("SearchService", "searchMusicPaged failed: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    /**
     * Fetches the next page using a continuation token returned by [searchMusicPaged].
     * Returns the new results AND the token for the page after that (null if no more pages).
     */
    suspend fun fetchContinuation(continuationToken: String, songsOnly: Boolean = false): Pair<List<SearchResult>, String?> = withContext(Dispatchers.IO) {
        try {
            val safeToken = continuationToken.replace("\\", "\\\\").replace("\"", "\\\"")
            val body = """{"context":$baseContextJson,"continuation":"$safeToken"}"""
            val json = executeSearchRequest(body) ?: return@withContext Pair(emptyList(), null)
            val root = JsonParser.parseString(json).asJsonObject
            val collected = mutableListOf<SearchResult>()
            findMusicListItemRenderers(root, collected, songsOnly)
            val nextToken = findFirstContinuationToken(root)
            val resolvedToken = if (nextToken == continuationToken) null else nextToken
            Pair(collected.distinctBy { it.id }, resolvedToken)
        } catch (e: Exception) {
            Log.e("SearchService", "fetchContinuation failed: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    private fun searchYouTube(query: String, songsOnly: Boolean, maxPages: Int = 1): List<SearchResult> {
        val safeQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
        val paramsField = if (songsOnly) ",\"params\":\"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D\"" else ""
        val body = """{"context":$baseContextJson,"query":"$safeQuery"$paramsField}"""

        val collected = mutableListOf<SearchResult>()
        val seenTokens = mutableSetOf<String>()
        var continuationToken: String? = null

        val firstPageJson = executeSearchRequest(body) ?: return emptyList()
        try {
            val root = JsonParser.parseString(firstPageJson).asJsonObject
            findMusicListItemRenderers(root, collected, songsOnly)
            continuationToken = findFirstContinuationToken(root)
            continuationToken?.let { seenTokens.add(it) }
        } catch (e: Exception) {
            Log.e("SearchService", "Error parsing initial YT Music response", e)
            return emptyList()
        }

        var page = 1
        val maxResults = maxPages * 25 // ~25 results per page
        while (continuationToken != null && page < maxPages && collected.size < maxResults) {
            val safeToken = continuationToken.replace("\\", "\\\\").replace("\"", "\\\"")
            val continuationBody = """{"context":$baseContextJson,"continuation":"$safeToken"}"""
            val continuationJson = executeSearchRequest(continuationBody) ?: break

            try {
                val continuationRoot = JsonParser.parseString(continuationJson).asJsonObject
                findMusicListItemRenderers(continuationRoot, collected, songsOnly)
                val nextToken = findFirstContinuationToken(continuationRoot)
                continuationToken = if (nextToken == null || !seenTokens.add(nextToken)) {
                    null
                } else {
                    nextToken
                }
            } catch (e: Exception) {
                Log.e("SearchService", "Error parsing continuation page $page", e)
                break
            }

            page++
        }

        val uniqueResults = collected
            .distinctBy { it.id }
            .take(maxPages * 25)
        return uniqueResults
    }

    /**
     * Fetches the YouTube Music home feed using the browse endpoint (FEmusic_home).
     * Returns real curated home content — trending, new releases, charts — the same
     * data that powers the YT Music homepage, in one fast API call.
     */
    suspend fun fetchHomeFeed(): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val body = """{"context":$baseContextJson,"browseId":"FEmusic_home"}"""
            val request = okhttp3.Request.Builder()
                .url(browseUrl)
                .post(body.toRequestBody("application/json".toMediaType()))
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .addHeader("X-YouTube-Client-Name", "67")
                .addHeader("X-YouTube-Client-Version", "1.20240101.01.00")
                .addHeader("Origin", "https://music.youtube.com")
                .addHeader("Referer", "https://music.youtube.com/")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("SearchService", "Home feed responded ${response.code}, will fall back to search")
                return@withContext emptyList()
            }
            val json = response.body?.string() ?: return@withContext emptyList()
            val root = JsonParser.parseString(json).asJsonObject
            val collected = mutableListOf<SearchResult>()
            findMusicListItemRenderers(root, collected, songsOnly = false)
            findMusicTwoRowItemRenderers(root, collected)
            val results = collected.filter { it.isPlayable }.distinctBy { it.id }
            results
        } catch (e: Exception) {
            Log.e("SearchService", "fetchHomeFeed failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun findMusicTwoRowItemRenderers(element: JsonElement, results: MutableList<SearchResult>) {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("musicTwoRowItemRenderer")) {
                parseMusicTwoRowItemRenderer(obj.getAsJsonObject("musicTwoRowItemRenderer"))?.let { results.add(it) }
            } else {
                for ((_, value) in obj.entrySet()) findMusicTwoRowItemRenderers(value, results)
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) findMusicTwoRowItemRenderers(item, results)
        }
    }

    private fun parseMusicTwoRowItemRenderer(renderer: JsonObject): SearchResult? {
        return try {
            val videoId = renderer
                .getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("watchEndpoint")
                ?.get("videoId")?.asString
                ?: renderer.getAsJsonObject("overlay")
                    ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                    ?.getAsJsonObject("content")
                    ?.getAsJsonObject("musicPlayButtonRenderer")
                    ?.getAsJsonObject("playNavigationEndpoint")
                    ?.getAsJsonObject("watchEndpoint")
                    ?.get("videoId")?.asString
            if (videoId.isNullOrBlank()) return null

            val title = renderer.getAsJsonObject("title")
                ?.getAsJsonArray("runs")
                ?.firstOrNull()?.asJsonObject
                ?.get("text")?.asString ?: return null

            // Subtitle runs look like: ["Song", " • ", "Artist"] — skip the type label
            val subtitleRuns = renderer.getAsJsonObject("subtitle")
                ?.getAsJsonArray("runs")
            val innertubTypeLabels = setOf("song", "single", "ep", "album", "artist",
                                           "playlist", "podcast", "episode", "video")
            val artist = subtitleRuns
                ?.mapNotNull { it?.asJsonObject?.get("text")?.asString }
                ?.firstOrNull { t ->
                    val trimmed = t.trim()
                    trimmed.isNotEmpty()
                        && trimmed != "•"
                        && trimmed.lowercase() !in innertubTypeLabels
                } ?: ""

            val thumbnail = renderer.getAsJsonObject("thumbnailRenderer")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.lastOrNull()?.asJsonObject
                ?.get("url")?.asString
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

            SearchResult(
                id = videoId,
                title = title,
                artist = artist,
                duration = "",
                thumbnailUrl = thumbnail,
                videoUrl = "https://music.youtube.com/watch?v=$videoId",
                audioUrl = null,
                itemType = "song",
                isPlayable = true
            )
        } catch (e: Exception) { null }
    }

    private fun executeSearchRequest(body: String): String? {
        val request = okhttp3.Request.Builder()
            .url(searchUrl)
            .post(body.toRequestBody("application/json".toMediaType()))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("X-YouTube-Client-Name", "67")
            .addHeader("X-YouTube-Client-Version", "1.20240101.01.00")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("Referer", "https://music.youtube.com/")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.w("SearchService", "YT Music search responded ${response.code}")
            return null
        }

        return response.body?.string()
    }

    // Recursively walk the JSON tree looking for musicResponsiveListItemRenderer objects
    private fun findMusicListItemRenderers(
        element: JsonElement,
        results: MutableList<SearchResult>,
        songsOnly: Boolean
    ) {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("musicResponsiveListItemRenderer")) {
                parseMusicListItemRenderer(
                    renderer = obj.getAsJsonObject("musicResponsiveListItemRenderer"),
                    songsOnly = songsOnly
                )?.let { results.add(it) }
            } else {
                for ((_, value) in obj.entrySet()) {
                    findMusicListItemRenderers(value, results, songsOnly)
                }
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                findMusicListItemRenderers(item, results, songsOnly)
            }
        }
    }

    private fun parseMusicListItemRenderer(renderer: JsonObject, songsOnly: Boolean): SearchResult? {
        return try {
            val flexColsCheck = renderer.getAsJsonArray("flexColumns")
            val typeText = if (flexColsCheck != null && flexColsCheck.size() > 1) {
                flexColsCheck[1]?.asJsonObject
                    ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                    ?.firstOrNull()?.asJsonObject
                    ?.get("text")?.asString
                    ?.trim()
                    ?.lowercase()
                    .orEmpty()
            } else {
                ""
            }

            // For song-only mode, skip non-song entities like playlists/albums/artists/videos.
            // NOTE: when using the songsOnly API param, typeText is the artist name not the type,
            // so we do NOT filter by typeText here. Post-filtering happens in the ViewModel.
            if (songsOnly) {
                // only filter out items that are explicitly non-song types like episode/podcast
                if (typeText in setOf("episode", "podcast")) return null
            }

            // videoId from the overlay play button endpoint
            val videoId = renderer
                .getAsJsonObject("overlay")
                ?.getAsJsonObject("musicItemThumbnailOverlayRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("musicPlayButtonRenderer")
                ?.getAsJsonObject("playNavigationEndpoint")
                ?.getAsJsonObject("watchEndpoint")
                ?.get("videoId")?.asString
                ?: renderer.getAsJsonArray("flexColumns")
                    ?.firstOrNull()?.asJsonObject
                    ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                    ?.firstOrNull()?.asJsonObject
                    ?.getAsJsonObject("navigationEndpoint")
                    ?.getAsJsonObject("watchEndpoint")
                    ?.get("videoId")?.asString

            val browseId = renderer.getAsJsonArray("flexColumns")
                ?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("navigationEndpoint")
                ?.getAsJsonObject("browseEndpoint")
                ?.get("browseId")
                ?.asString

            if (videoId.isNullOrBlank() && browseId.isNullOrBlank()) return null

            val resultId = if (!videoId.isNullOrBlank()) {
                videoId
            } else {
                "browse_$browseId"
            }

            val flexCols = renderer.getAsJsonArray("flexColumns") ?: return null

            val title = if (flexCols.size() > 0) flexCols[0]?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.firstOrNull()?.asJsonObject
                ?.get("text")?.asString else null
            if (title == null) return null

            // The subtitle column runs look like: ["Song", " • ", "Artist Name", " • ", "Year"]
            // We must skip the type label ("Song", "Album", etc.) and separators to get the real artist.
            val typeLabels = setOf("song", "single", "ep", "album", "artist",
                                   "playlist", "podcast", "episode", "video", "browse")
            val artistRuns = if (flexCols.size() > 1) flexCols[1]?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs") else null
            val artistName = artistRuns
                ?.mapNotNull { it?.asJsonObject?.get("text")?.asString }
                ?.firstOrNull { t ->
                    val trimmed = t.trim()
                    trimmed.isNotEmpty()
                        && trimmed != "•"
                        && trimmed.lowercase() !in typeLabels
                } ?: ""

            val thumbnail = renderer.getAsJsonObject("thumbnail")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.lastOrNull()?.asJsonObject
                ?.get("url")?.asString
                ?: if (!videoId.isNullOrBlank()) "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" else ""

            val duration = renderer.getAsJsonArray("fixedColumns")
                ?.firstOrNull()?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFixedColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.firstOrNull()?.asJsonObject
                ?.get("text")?.asString ?: ""

            SearchResult(
                id = resultId,
                title = title,
                artist = artistName,
                duration = duration,
                thumbnailUrl = thumbnail,
                videoUrl = if (!videoId.isNullOrBlank()) {
                    "https://music.youtube.com/watch?v=$videoId"
                } else {
                    ""
                },
                audioUrl = null,
                itemType = typeText.ifBlank { if (!videoId.isNullOrBlank()) "song" else "browse" },
                isPlayable = !videoId.isNullOrBlank()
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Finds the search paging continuation token.
     * For songsOnly responses: token is at musicShelfRenderer.continuations[0].nextContinuationData.continuation
     * For continuation responses: similar structure in musicShelfContinuation
     */
    private fun findFirstContinuationToken(root: JsonElement): String? {
        if (!root.isJsonObject) return null
        val obj = root.asJsonObject

        // Path for initial songsOnly search response
        val fromSearch = obj.getAsJsonObject("contents")
            ?.getAsJsonObject("tabbedSearchResultsRenderer")
            ?.getAsJsonArray("tabs")?.get(0)?.asJsonObject
            ?.getAsJsonObject("tabRenderer")?.getAsJsonObject("content")
            ?.getAsJsonObject("sectionListRenderer")
            ?.getAsJsonArray("contents")
            ?.firstOrNull { it.asJsonObject.has("musicShelfRenderer") }
            ?.asJsonObject?.getAsJsonObject("musicShelfRenderer")
            ?.getAsJsonArray("continuations")
            ?.get(0)?.asJsonObject
            ?.deepGetToken()
        if (fromSearch != null) return fromSearch

        // Path for continuation responses (musicShelfContinuation)
        val fromContinuation = obj.getAsJsonObject("continuationContents")
            ?.getAsJsonObject("musicShelfContinuation")
            ?.getAsJsonArray("continuations")
            ?.get(0)?.asJsonObject
            ?.deepGetToken()
        if (fromContinuation != null) return fromContinuation

        // Fallback: full recursive scan skipping song item renderer branches
        return fallbackScan(root)
    }

    private fun JsonObject.deepGetToken(): String? {
        // nextContinuationData.continuation
        val t1 = getAsJsonObject("nextContinuationData")?.get("continuation")
            ?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.length > 50 }
        if (t1 != null) return t1
        // reloadContinuationData.continuation
        val t2 = getAsJsonObject("reloadContinuationData")?.get("continuation")
            ?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.length > 50 }
        if (t2 != null) return t2
        return null
    }

    private fun fallbackScan(element: JsonElement): String? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            val direct = obj.get("continuation")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString?.takeIf { it.length > 50 }
            if (direct != null) return direct
            for ((key, value) in obj.entrySet()) {
                if (key in SKIP_KEYS) continue
                val t = fallbackScan(value)
                if (t != null) return t
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                val t = fallbackScan(item)
                if (t != null) return t
            }
        }
        return null
    }

    companion object {
        /** Keys inside song/video item renderers that hold browse (not paging) tokens. */
        private val SKIP_KEYS = setOf(
            "musicResponsiveListItemRenderer",
            "musicTwoRowItemRenderer",
            "navigationEndpoint",
            "continuationEndpoint",
            "watchEndpoint",
            "browseEndpoint",
            "overlay",
            "flexColumns",
            "fixedColumns",
            "thumbnail"
        )
    }
}