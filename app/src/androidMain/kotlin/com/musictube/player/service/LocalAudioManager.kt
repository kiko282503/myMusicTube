package com.musictube.player.service

import android.content.Context
import android.util.Log
import com.musictube.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LocalAudioManager(private val context: Context) {

    companion object {
        private const val TAG = "LocalAudioManager"
        private const val AUDIO_CACHE_DIR = "music_cache"
    }

    private val cacheDir: File by lazy {
        File(context.externalCacheDir, AUDIO_CACHE_DIR).apply { if (!exists()) mkdirs() }
    }

    fun getSampleAudioFiles(): List<Song> = listOf(
        Song(id = "sample_1", title = "Demo Track 1", artist = "Sample Audio",
            album = "Fallback Collection", duration = 180000L,
            filePath = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-_bowls.mp3",
            isLocal = false, thumbnailUrl = createPlaceholderThumbnail("Demo Track 1", "Sample Audio")),
        Song(id = "sample_2", title = "Demo Track 2", artist = "Sample Audio",
            album = "Fallback Collection", duration = 210000L,
            filePath = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
            isLocal = false, thumbnailUrl = createPlaceholderThumbnail("Demo Track 2", "Sample Audio")),
        Song(id = "sample_3", title = "Demo Track 3", artist = "Sample Audio",
            album = "Fallback Collection", duration = 195000L,
            filePath = "https://commondatastorage.googleapis.com/codeskulptor-assets/Eban_Schletter_-_Dance_of_Otherworldly_Ambiance.mp3",
            isLocal = false, thumbnailUrl = createPlaceholderThumbnail("Demo Track 3", "Sample Audio"))
    )

    suspend fun createLocalVersion(title: String, artist: String, videoId: String): Song? {
        return withContext(Dispatchers.IO) {
            try {
                val sample = getSampleAudioFiles().random()
                val workingUrl = findWorkingAudioUrl(sample.filePath) ?: findWorkingBackupUrl()
                if (workingUrl != null) {
                    Song(id = "local_$videoId", title = title, artist = artist,
                        album = "MusicTube Demo", duration = sample.duration, filePath = workingUrl,
                        isLocal = false, thumbnailUrl = createPlaceholderThumbnail(title, artist))
                } else { null }
            } catch (e: Exception) { Log.e(TAG, "Failed to create local version", e); null }
        }
    }

    private suspend fun findWorkingAudioUrl(url: String?): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isNullOrEmpty()) return@withContext null
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "HEAD"; conn.connectTimeout = 3000; conn.readTimeout = 3000
                conn.setRequestProperty("User-Agent", "MusicTube/1.0")
                val code = conn.responseCode; conn.disconnect()
                if (code in 200..299) url else null
            } catch (e: Exception) { null }
        }
    }

    private suspend fun findWorkingBackupUrl(): String? {
        val backupUrls = listOf(
            "https://archive.org/download/testmp3testfile/mpthreetest.mp3",
            "https://mdn.github.io/learning-area/html/multimedia-and-embedding/video-and-audio-content/viper.mp3",
            "https://www.w3schools.com/html/horse.mp3"
        )
        for (url in backupUrls) { if (findWorkingAudioUrl(url) != null) return url }
        return null
    }

    fun createPlaceholderThumbnail(title: String, artist: String): String {
        val enc = java.net.URLEncoder.encode("$title by $artist", "UTF-8")
        return "https://via.placeholder.com/300x300/1976D2/FFFFFF?text=$enc"
    }

    fun hasCachedAudio(songId: String): Boolean {
        val f = File(cacheDir, "$songId.mp3"); return f.exists() && f.length() > 0
    }

    fun getCachedAudioPath(songId: String): String? {
        val f = File(cacheDir, "$songId.mp3"); return if (f.exists()) f.absolutePath else null
    }
}
