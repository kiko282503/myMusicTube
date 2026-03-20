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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val baseContextJson = """{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}}"""
    private val searchUrl = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"

    // songsOnly=true adds the YouTube Music "Songs" filter so only individual tracks are returned
    suspend fun searchMusic(query: String, songsOnly: Boolean = false): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            searchYouTube(query, songsOnly)
        } catch (e: Exception) {
            Log.e("SearchService", "YouTube search failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun searchYouTube(query: String, songsOnly: Boolean): List<SearchResult> {
        Log.d("SearchService", "Searching YouTube Music for: $query (songsOnly=$songsOnly)")

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
        val maxPages = 6
        val maxResults = 200
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
            .take(maxResults)
        Log.d("SearchService", "Extracted ${uniqueResults.size} unique results across $page pages")
        return uniqueResults
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
            if (songsOnly) {
                if (typeText.isNotEmpty() && typeText != "song") return null
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

            val artist = if (flexCols.size() > 1) flexCols[1]?.asJsonObject
                ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")
                ?.getAsJsonArray("runs")
                ?.firstOrNull()?.asJsonObject
                ?.get("text")?.asString else null
            val artistName = artist ?: ""

            val thumbnail = renderer.getAsJsonObject("thumbnail")
                ?.getAsJsonObject("musicThumbnailRenderer")
                ?.getAsJsonObject("thumbnail")
                ?.getAsJsonArray("thumbnails")
                ?.lastOrNull()?.asJsonObject
                ?.get("url")?.asString
                ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

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

    private fun findFirstContinuationToken(element: JsonElement): String? {
        if (element.isJsonObject) {
            val obj = element.asJsonObject

            val token = obj.getAsJsonObject("continuationEndpoint")
                ?.getAsJsonObject("continuationCommand")
                ?.get("token")
                ?.asString
            if (!token.isNullOrBlank()) return token

            for ((_, value) in obj.entrySet()) {
                val nested = findFirstContinuationToken(value)
                if (nested != null) return nested
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                val nested = findFirstContinuationToken(item)
                if (nested != null) return nested
            }
        }

        return null
    }
}