package com.musictube.player.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeAudioExtractor @Inject constructor() {

    companion object {
        private const val TAG = "YouTubeAudioExtractor"
    }

    /**
     * Extract direct audio stream URL from YouTube video
     * This bypasses the WebView and YouTube Music restrictions
     */
    suspend fun extractAudioUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to extract audio URL for video: $videoId")
                
                // Method 1: Try yt-dlp style extraction using YouTube's player API
                val playerUrl = extractFromPlayerApi(videoId)
                if (playerUrl != null) {
                    Log.d(TAG, "Successfully extracted audio URL via player API")
                    return@withContext playerUrl
                }
                
                // Method 2: Try using YouTube's innertube API
                val innertubeUrl = extractFromInnertube(videoId)
                if (innertubeUrl != null) {
                    Log.d(TAG, "Successfully extracted audio URL via innertube API")
                    return@withContext innertubeUrl
                }
                
                // Method 3: Try simple proxy approach
                val proxyUrl = trySimpleProxy(videoId)
                if (proxyUrl != null) {
                    Log.d(TAG, "Successfully found simple proxy URL")
                    return@withContext proxyUrl
                }
                
                // Method 4: Try direct video info API
                val videoUrl = "https://www.youtube.com/watch?v=$videoId"
                val extractedUrl = extractStreamUrl(videoUrl)
                if (extractedUrl != null) {
                    Log.d(TAG, "Successfully extracted audio URL via video info API")
                    return@withContext extractedUrl
                }
                
                Log.w(TAG, "All extraction methods failed for video: $videoId")
                return@withContext null
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to extract audio URL for $videoId", e)
                null
            }
        }
    }

    /** Extract audio URL using YouTube Music API specifically */
    suspend fun extractFromYouTubeMusic(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting YouTube Music API extraction for: $videoId")

                // Try YouTube Music WEB_REMIX client
                val body = """{"videoId":"$videoId","context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20220918.00.00","hl":"en","gl":"US"}}}""".trimIndent()
                val connection = URL("https://music.youtube.com/youtubei/v1/player").openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.setRequestProperty("Origin", "https://music.youtube.com")
                connection.setRequestProperty("Referer", "https://music.youtube.com/")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true
                connection.outputStream.write(body.toByteArray())

                if (connection.responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val url = parsePlayerResponse(response)
                    if (url != null) {
                        Log.d(TAG, "YouTube Music WEB_REMIX extraction succeeded")
                        return@withContext url
                    }
                }

                // Fallback: try ANDROID_MUSIC client
                val body2 = """{"videoId":"$videoId","context":{"client":{"clientName":"ANDROID_MUSIC","clientVersion":"5.29.52","androidSdkVersion":30,"hl":"en","gl":"US"}}}""".trimIndent()
                val conn2 = URL("https://music.youtube.com/youtubei/v1/player").openConnection() as HttpURLConnection
                conn2.requestMethod = "POST"
                conn2.setRequestProperty("Content-Type", "application/json")
                conn2.setRequestProperty("User-Agent", "com.google.android.apps.youtube.music/5.29.52 (Linux; U; Android 11) gzip")
                conn2.connectTimeout = 10000
                conn2.readTimeout = 10000
                conn2.doOutput = true
                conn2.outputStream.write(body2.toByteArray())

                if (conn2.responseCode == 200) {
                    val response2 = BufferedReader(InputStreamReader(conn2.inputStream)).use { it.readText() }
                    val url2 = parsePlayerResponse(response2)
                    if (url2 != null) {
                        Log.d(TAG, "YouTube Music ANDROID_MUSIC extraction succeeded")
                        return@withContext url2
                    }
                }

                Log.w(TAG, "YouTube Music extraction failed for: $videoId")
                null
            } catch (e: Exception) {
                Log.e(TAG, "YouTube Music extraction exception for $videoId", e)
                null
            }
        }
    }

    private fun extractFromPlayerApi(videoId: String): String? {
        try {
            Log.d(TAG, "Trying YouTube player API for: $videoId")
            
            val playerUrl = "https://www.youtube.com/youtubei/v1/player"
            val body = """
                {
                    "videoId": "$videoId",
                    "context": {
                        "client": {
                            "clientName": "ANDROID",
                            "clientVersion": "17.31.35",
                            "androidSdkVersion": 30,
                            "hl": "en",
                            "gl": "US"
                        }
                    }
                }
            """.trimIndent()
            
            val connection = URL(playerUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "com.google.android.youtube/17.31.35 (Linux; U; Android 11) gzip")
            connection.doOutput = true
            
            connection.outputStream.write(body.toByteArray())
            
            if (connection.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                return parsePlayerResponse(response)
            } else {
                Log.w(TAG, "Player API returned status: ${connection.responseCode}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in player API extraction", e)
        }
        return null
    }

    private fun extractFromInnertube(videoId: String): String? {
        try {
            Log.d(TAG, "Trying YouTube innertube API for: $videoId")
            
            val innertubeUrl = "https://www.youtube.com/youtubei/v1/player?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"
            val body = """
                {
                    "videoId": "$videoId",
                    "context": {
                        "client": {
                            "clientName": "WEB",
                            "clientVersion": "2.20240304.00.00"
                        }
                    },
                    "playbackContext": {
                        "contentPlaybackContext": {
                            "html5Preference": "HTML5_PREF_WANTS"
                        }
                    }
                }
            """.trimIndent()
            
            val connection = URL(innertubeUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.doOutput = true
            
            connection.outputStream.write(body.toByteArray())
            
            if (connection.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                return parsePlayerResponse(response)
            } else {
                Log.w(TAG, "Innertube API returned status: ${connection.responseCode}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in innertube API extraction", e)
        }
        return null
    }

    private fun parsePlayerResponse(response: String): String? {
        try {
            // Look for streaming data with audio-only formats
            val streamingDataRegex = "\"streamingData\":\\{[^}]*\"adaptiveFormats\":\\[([^]]+)]".toRegex()
            val streamingMatch = streamingDataRegex.find(response)
            
            if (streamingMatch != null) {
                val formatsData = streamingMatch.groupValues[1]
                
                // Look for audio-only streams (itag 140 = m4a, itag 251 = webm)
                val audioFormats = listOf("140", "251", "250", "249")
                
                for (itag in audioFormats) {
                    val urlRegex = "\"itag\":$itag[^}]*\"url\":\"([^\"]+)\"".toRegex()
                    val urlMatch = urlRegex.find(formatsData)
                    
                    if (urlMatch != null) {
                        val audioUrl = urlMatch.groupValues[1]
                            .replace("\\u0026", "&")
                            .replace("\\/", "/")
                        
                        if (audioUrl.startsWith("http")) {
                            Log.d(TAG, "Found audio stream with itag $itag")
                            return audioUrl
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing player response", e)
        }
        return null
    }

    private fun extractStreamUrl(videoUrl: String): String? {
        try {
            // Method 1: Try to extract from YouTube's get_video_info endpoint
            val videoId = extractVideoId(videoUrl) ?: return null
            
            // YouTube's internal API endpoint for video info
            val infoUrl = "https://www.youtube.com/get_video_info?video_id=$videoId&el=embedded"
            
            val connection = URL(infoUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            
            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                return parseStreamUrl(response)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting stream URL", e)
        }
        return null
    }

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([\\w-]+)".toRegex(),
            "youtube\\.com/embed/([\\w-]+)".toRegex()
        )
        
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun parseStreamUrl(response: String): String? {
        try {
            // Parse the response to find audio stream URLs
            val urlEncodedData = URLDecoder.decode(response, "UTF-8")
            
            // Look for adaptive formats (audio-only streams)
            val streamRegex = "\"url\":\"([^\"]+)\".*?\"mimeType\":\"audio".toRegex()
            val matches = streamRegex.findAll(urlEncodedData)
            
            for (match in matches) {
                val streamUrl = match.groupValues[1]
                    .replace("\\u0026", "&")
                    .replace("\\/", "/")
                
                if (streamUrl.isNotEmpty() && streamUrl.startsWith("http")) {
                    return streamUrl
                }
            }
            
            // Alternative: Look for any audio stream in different format
            val altStreamRegex = "url=([^&]+).*?type=audio".toRegex()
            val altMatches = altStreamRegex.findAll(urlEncodedData)
            
            for (match in altMatches) {
                val streamUrl = URLDecoder.decode(match.groupValues[1], "UTF-8")
                if (streamUrl.isNotEmpty() && streamUrl.startsWith("http")) {
                    Log.d(TAG, "Found alternative audio stream")
                    return streamUrl
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing stream URL", e)
        }
        return null
    }

    /**
     * Simple test method using a basic YouTube proxy approach
     */
    private fun trySimpleProxy(videoId: String): String? {
        try {
            // Try using YouTube's basic audio proxy (sometimes works for apps)
            val proxyUrl = "https://www.youtube.com/api/manifest/dash/id/$videoId/source/youtube/as/fmp4_audio_clear,fmp4_sd_hd_clear/requiressl/yes"
            
            val connection = URL(proxyUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD" // Just check if it exists
            connection.setRequestProperty("User-Agent", "MusicTube/1.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                Log.d(TAG, "Simple proxy URL available")
                return proxyUrl
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "Simple proxy failed: ${e.message}")
        }
        return null
    }

    /**
     * Fallback method using different extraction approach
     */
    suspend fun extractAudioUrlFallback(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Method 2: Use YouTube's OEmbed API as fallback
                val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
                
                val connection = URL(oembedUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val json = JSONObject(response)
                    
                    // This won't give us direct stream URL, but confirms video exists
                    val title = json.optString("title")
                    Log.d(TAG, "Video confirmed: $title")
                }
                
                // For now, return null - this would need more sophisticated extraction
                null
                
            } catch (e: Exception) {
                Log.e(TAG, "Fallback extraction failed", e)
                null
            }
        }
    }
}