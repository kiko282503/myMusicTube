package com.musictube.player.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import com.musictube.player.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "LocalAudioManager"
        private const val AUDIO_CACHE_DIR = "music_cache"
    }
    
    private val cacheDir: File by lazy {
        File(context.externalCacheDir, AUDIO_CACHE_DIR).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Get list of sample audio files that can be played without restrictions
     * This provides reliable background playback for demonstration
     */
    fun getSampleAudioFiles(): List<Song> {
        return listOf(
            Song(
                id = "sample_1",
                title = "Demo Track 1",
                artist = "Sample Audio",
                album = "Fallback Collection",
                duration = 180000L,
                filePath = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-_bowls.mp3",
                isLocal = false,
                thumbnailUrl = createPlaceholderThumbnail("Demo Track 1", "Sample Audio")
            ),
            Song(
                id = "sample_2", 
                title = "Demo Track 2",
                artist = "Sample Audio",
                album = "Fallback Collection",
                duration = 210000L,
                filePath = "https://commondatastorage.googleapis.com/codeskulptor-assets/Epoq-Lepidoptera.ogg",
                isLocal = false,
                thumbnailUrl = createPlaceholderThumbnail("Demo Track 2", "Sample Audio")
            ),
            Song(
                id = "sample_3",
                title = "Demo Track 3",
                artist = "Sample Audio",
                album = "Fallback Collection",
                duration = 195000L,
                filePath = "https://commondatastorage.googleapis.com/codeskulptor-assets/Eban_Schletter_-_Dance_of_Otherworldly_Ambiance.mp3",
                isLocal = false,
                thumbnailUrl = createPlaceholderThumbnail("Demo Track 3", "Sample Audio")
            )
        )
    }
    
    /**
     * Create a local version of a song that can play in background
     * For demo purposes, maps YouTube requests to sample audio
     */
    suspend fun createLocalVersion(title: String, artist: String, videoId: String): Song? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating local version for: $title by $artist")
                
                // Get all available sample audio files
                val sampleFiles = getSampleAudioFiles()
                
                // Try to pick a sample that hasn't been used recently or pick randomly
                val selectedSample = sampleFiles.random()
                
                // Test the selected sample URL to make sure it works
                val workingUrl = findWorkingAudioUrl(selectedSample.filePath) ?: run {
                    Log.w(TAG, "Primary sample URL failed, trying alternatives")
                    findWorkingBackupUrl()
                }
                
                if (workingUrl != null) {
                    // Create a local song with the requested metadata but working audio
                    val localSong = Song(
                        id = "local_${videoId}",
                        title = title,
                        artist = artist,
                        album = "MusicTube Demo",
                        duration = selectedSample.duration,
                        filePath = workingUrl,
                        isLocal = false,
                        thumbnailUrl = createPlaceholderThumbnail(title, artist)
                    )
                    
                    Log.i(TAG, "Successfully created local version for: $title with URL: $workingUrl")
                    localSong
                } else {
                    Log.e(TAG, "No working audio URLs found")
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create local version", e)
                null
            }
        }
    }
    
    /**
     * Test if an audio URL is accessible
     */
    private suspend fun findWorkingAudioUrl(url: String?): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isNullOrEmpty()) return@withContext null
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.setRequestProperty("User-Agent", "MusicTube/1.0")
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                if (responseCode in 200..299) {
                    Log.d(TAG, "URL is accessible: $url")
                    url
                } else {
                    Log.w(TAG, "URL returned $responseCode: $url") 
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "URL test failed for $url: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Backup URLs if primary samples fail
     */
    private suspend fun findWorkingBackupUrl(): String? {
        val backupUrls = listOf(
            // Internet Archive - Public domain audio
            "https://archive.org/download/testmp3testfile/mpthreetest.mp3",
            // Mozilla's test audio
            "https://mdn.github.io/learning-area/html/multimedia-and-embedding/video-and-audio-content/viper.mp3",
            // W3C test audio
            "https://www.w3schools.com/html/horse.mp3"
        )
        
        for (url in backupUrls) {
            val workingUrl = findWorkingAudioUrl(url)
            if (workingUrl != null) {
                Log.i(TAG, "Found working backup URL: $workingUrl")
                return workingUrl
            }
        }
        
        Log.e(TAG, "All backup URLs failed")
        return null
    }
    
    private fun createPlaceholderThumbnail(title: String, artist: String): String {
        // Generate a placeholder thumbnail URL using the title and artist
        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
        val encodedArtist = java.net.URLEncoder.encode(artist, "UTF-8")
        
        // Use a free placeholder image service
        return "https://via.placeholder.com/300x300/1976D2/FFFFFF?text=${encodedTitle}+by+${encodedArtist}"
    }
    
    /**
     * Check if we have cached audio for a specific song
     */
    fun hasCachedAudio(songId: String): Boolean {
        val cacheFile = File(cacheDir, "$songId.mp3")
        return cacheFile.exists() && cacheFile.length() > 0
    }
    
    /**
     * Get cached audio file path
     */
    fun getCachedAudioPath(songId: String): String? {
        val cacheFile = File(cacheDir, "$songId.mp3")
        return if (cacheFile.exists()) cacheFile.absolutePath else null
    }
    
    /**
     * Download and cache audio file for offline use
     */
    suspend fun cacheAudio(song: Song): String? {
        return withContext(Dispatchers.IO) {
            try {
                val audioUrl = song.filePath ?: song.url ?: return@withContext null
                val cacheFile = File(cacheDir, "${song.id}.mp3")
                
                if (cacheFile.exists()) {
                    return@withContext cacheFile.absolutePath
                }
                
                Log.d(TAG, "Downloading audio for caching: ${song.title}")
                
                val connection = URL(audioUrl).openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "MusicTube/1.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                
                if (connection.responseCode == 200) {
                    connection.inputStream.use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    Log.i(TAG, "Successfully cached audio: ${song.title}")
                    return@withContext cacheFile.absolutePath
                } else {
                    Log.w(TAG, "Failed to download audio: ${connection.responseCode}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error caching audio", e)
            }
            null
        }
    }
    
    /**
     * Get audio metadata from file
     */
    suspend fun getAudioMetadata(filePath: String): Map<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(filePath)
                
                val metadata = mapOf(
                    "duration" to (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"),
                    "title" to (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown"),
                    "artist" to (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown"),
                    "album" to (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown")
                )
                
                retriever.release()
                metadata
                
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting metadata", e)
                null
            }
        }
    }
    
    /**
     * Clear cache to free up space
     */
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            try {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                        Log.d(TAG, "Deleted cached file: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
            }
        }
    }
}