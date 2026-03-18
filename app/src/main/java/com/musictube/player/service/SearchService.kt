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

        val safeQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")        // EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D is the YouTube Music "Songs" search filter
        val paramsField = if (songsOnly) ",\"params\":\"EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D\"" else ""
        val body = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00","hl":"en","gl":"US"}},"query":"$safeQuery"$paramsField}"""

        val request = okhttp3.Request.Builder()
            .url("https://music.youtube.com/youtubei/v1/search?prettyPrint=false")
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
            return emptyList()
        }

        val json = response.body?.string() ?: return emptyList()
        Log.d("SearchService", "Got YT Music response, length: ${json.length}")

        val results = mutableListOf<SearchResult>()
        try {
            val root = JsonParser.parseString(json).asJsonObject
            findMusicListItemRenderers(root, results)
        } catch (e: Exception) {
            Log.e("SearchService", "Error parsing YT Music response", e)
        }
        Log.d("SearchService", "Extracted ${results.size} results")
        return results
    }

    // Recursively walk the JSON tree looking for musicResponsiveListItemRenderer objects
    private fun findMusicListItemRenderers(element: JsonElement, results: MutableList<SearchResult>) {
        if (results.size >= 20) return
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("musicResponsiveListItemRenderer")) {
                parseMusicListItemRenderer(obj.getAsJsonObject("musicResponsiveListItemRenderer"))?.let { results.add(it) }
            } else {
                for ((_, value) in obj.entrySet()) {
                    findMusicListItemRenderers(value, results)
                }
            }
        } else if (element.isJsonArray) {
            for (item in element.asJsonArray) {
                findMusicListItemRenderers(item, results)
            }
        }
    }

    private fun parseMusicListItemRenderer(renderer: JsonObject): SearchResult? {
        return try {
            // Check type from second flex column — skip playlists, albums, artists
            val flexColsCheck = renderer.getAsJsonArray("flexColumns")
            if (flexColsCheck != null && flexColsCheck.size() > 1) {
                val runs = flexColsCheck[1]?.asJsonObject
                    ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.getAsJsonObject("text")
                    ?.getAsJsonArray("runs")
                val typeText = runs?.firstOrNull()?.asJsonObject?.get("text")?.asString?.lowercase() ?: ""
                // Only keep items explicitly labelled "song" (or no type label).
                // This filters out Video, Episode, Playlist, Album, EP, Single, Artist, etc.
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
                ?: return null

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
                id = videoId,
                title = title,
                artist = artistName,
                duration = duration,
                thumbnailUrl = thumbnail,
                videoUrl = "https://music.youtube.com/watch?v=$videoId",
                audioUrl = null
            )
        } catch (e: Exception) {
            null
        }
    }
}