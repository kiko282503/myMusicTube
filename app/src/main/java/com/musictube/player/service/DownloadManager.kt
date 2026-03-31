package com.musictube.player.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadStatus {
    IDLE, DOWNLOADING, COMPLETED, FAILED
}

/**
 * Singleton service that manages downloads with a long-lived coroutine scope.
 * This ensures downloads continue even if the UI is navigated away.
 * NOT tied to ViewModel lifecycle.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloader: OkHttpDownloader,
    private val youTubeStreamService: YouTubeStreamService,
    private val searchService: SearchService,
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager
) {
    // Counts downloads that are queued or running; used to start/stop the foreground service.
    private val activeDownloadCount = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val _downloadErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadErrors: StateFlow<Map<String, String>> = _downloadErrors.asStateFlow()

    // Tracks every download ever enqueued this session (id → SearchResult) so the Downloads
    // screen can show title/artist/thumbnail for all entries regardless of current search query.
    private val _downloadQueue = MutableStateFlow<Map<String, SearchResult>>(emptyMap())
    val downloadQueue: StateFlow<Map<String, SearchResult>> = _downloadQueue.asStateFlow()

    fun downloadSong(
        searchResult: SearchResult,
        playlistName: String = "Offline Downloads"
    ) {
        if (!searchResult.isPlayable) {
            Log.w("DownloadManager", "Attempted to download non-playable item: ${searchResult.id}")
            return
        }

        // Prevent duplicate downloads: skip if already downloading or completed
        val existingStatus = _downloadStatus.value[searchResult.id]
        if (existingStatus == DownloadStatus.DOWNLOADING || existingStatus == DownloadStatus.COMPLETED) {
            Log.i("DownloadManager", "Skipping duplicate download for ${searchResult.id} (status=$existingStatus)")
            return
        }

        // Register song details so Downloads screen can display title/artist/thumbnail
        _downloadQueue.value = _downloadQueue.value + (searchResult.id to searchResult)

        // Start the foreground service before the coroutine so the service is up
        // even if the download waits behind the semaphore.
        val count = activeDownloadCount.incrementAndGet()
        if (count == 1) startDownloadService()

        scope.launch {
            val statusMap = _downloadStatus.value.toMutableMap()
            statusMap[searchResult.id] = DownloadStatus.DOWNLOADING
            _downloadStatus.value = statusMap

            val progressMap = _downloadProgress.value.toMutableMap()
            progressMap[searchResult.id] = 0
            _downloadProgress.value = progressMap

            // Clear any previous errors
            val errorMap = _downloadErrors.value.toMutableMap()
            errorMap.remove(searchResult.id)
            _downloadErrors.value = errorMap

            try {
                Log.d("DownloadManager", "Starting download for ${searchResult.id}: ${searchResult.title}")

                val playlistId = getOrCreatePlaylist(playlistName)

                // Retry extraction up to 3 times with increasing delays to handle rate-limiting
                var audioUrl: String? = searchResult.audioUrl
                if (audioUrl == null) {
                    val maxAttempts = 3
                    for (attempt in 1..maxAttempts) {
                        audioUrl = extractBestAudioUrl(searchResult)
                        if (audioUrl != null) break
                        if (attempt < maxAttempts) {
                            val delayMs = attempt * 3000L
                            Log.w("DownloadManager", "Extraction attempt $attempt failed for ${searchResult.id}, retrying in ${delayMs}ms")
                            delay(delayMs)
                        }
                    }
                }
                // Last resort: use a CDN URL captured when the user previously played this
                // song via the WebView fallback (works on devices where innertube is blocked).
                if (audioUrl == null) {
                    audioUrl = playerManager.getCapturedAudioUrl(searchResult.id)
                    if (audioUrl != null)
                        Log.i("DownloadManager", "Using captured WebView audio URL for ${searchResult.id}")
                }
                if (audioUrl == null) throw IllegalStateException(
                    "Download failed. Please check your connection and try again.")

                Log.d("DownloadManager", "Downloading audio from: $audioUrl")

                val localPath = downloader.downloadAudio(
                    url = audioUrl,
                    fileNameHint = "${searchResult.title}_${searchResult.artist}"
                ) { progress ->
                    val map = _downloadProgress.value.toMutableMap()
                    map[searchResult.id] = progress
                    _downloadProgress.value = map
                    Log.d("DownloadManager", "Download progress for ${searchResult.id}: $progress%")
                }

                Log.d("DownloadManager", "Download complete, saved to: $localPath")

                val song = Song(
                    id = "yt_${searchResult.id}",
                    title = searchResult.title,
                    artist = searchResult.artist,
                    duration = parseDuration(searchResult.duration),
                    filePath = localPath,
                    url = audioUrl,
                    thumbnailUrl = searchResult.thumbnailUrl,
                    isLocal = true,
                    isDownloaded = true
                )

                musicRepository.addSongToPlaylist(playlistId, song)

                val completedStatus = _downloadStatus.value.toMutableMap()
                completedStatus[searchResult.id] = DownloadStatus.COMPLETED
                _downloadStatus.value = completedStatus

                val doneProgress = _downloadProgress.value.toMutableMap()
                doneProgress[searchResult.id] = 100
                _downloadProgress.value = doneProgress

                Log.i("DownloadManager", "Successfully downloaded: ${searchResult.title}")
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed for ${searchResult.id}: ${e.message}", e)

                val failedStatus = _downloadStatus.value.toMutableMap()
                failedStatus[searchResult.id] = DownloadStatus.FAILED
                _downloadStatus.value = failedStatus

                val errorMap = _downloadErrors.value.toMutableMap()
                errorMap[searchResult.id] = e.message ?: "Unknown error"
                _downloadErrors.value = errorMap
            } finally {
                // Decrement active count; stop the foreground service when all done.
                val remaining = activeDownloadCount.decrementAndGet()
                if (remaining <= 0) {
                    activeDownloadCount.set(0)
                    stopDownloadService()
                }
            }
        }
    }

    private suspend fun extractBestAudioUrl(searchResult: SearchResult): String? {
        return withContext(Dispatchers.IO) {
            // First, try the selected result directly.
            val primary = youTubeStreamService.extractAudioUrl(searchResult.id)
            if (primary != null) {
                Log.d("DownloadManager", "Using primary audio URL for ${searchResult.id}")
                return@withContext primary
            }

            // Fallback: search for the same song and try alternate video IDs.
            // Include artist in query and REQUIRE the result to match the artist
            // to prevent wrong-artist substitution (e.g. Pink Floyd instead of Korn cover).
            val query = when {
                searchResult.artist.isNotBlank() -> "${searchResult.artist} ${searchResult.title}"
                else -> "${searchResult.title} official audio"
            }

            Log.d("DownloadManager", "Primary failed, fallback search: $query")

            val artistLower = searchResult.artist.trim().lowercase()
            val alternates = searchService.searchMusic(query, songsOnly = true)
                .asSequence()
                .filter { it.isPlayable && it.id.isNotBlank() && it.id != searchResult.id }
                // Only accept results from the same artist (guards against wrong-artist substitution)
                .filter { candidate ->
                    artistLower.isEmpty() ||
                    candidate.artist.trim().lowercase().contains(artistLower) ||
                    artistLower.contains(candidate.artist.trim().lowercase())
                }
                .distinctBy { it.id }
                .take(8)
                .toList()

            for (candidate in alternates) {
                val alt = youTubeStreamService.extractAudioUrl(candidate.id)
                if (alt != null) {
                    Log.d("DownloadManager", "Found alternate audio for ${searchResult.id} using ${candidate.id} (${candidate.artist} - ${candidate.title})")
                    return@withContext alt
                }
            }

            // Last resort: silently load the YouTube Music page in a hidden WebView to capture
            // the audio CDN URL directly from the WebView's network traffic.
            // This works on devices where YouTube blocks all innertube API calls (po_token).
            Log.i("DownloadManager", "All API strategies failed for ${searchResult.id} — trying silent WebView capture")
            val silentUrl = playerManager.captureAudioUrlSilently(searchResult.id)
            if (silentUrl != null) {
                Log.i("DownloadManager", "Silent WebView capture succeeded for ${searchResult.id}")
                return@withContext silentUrl
            }

            Log.w("DownloadManager", "Could not extract audio URL for ${searchResult.id}")
            null
        }
    }

    private suspend fun getOrCreatePlaylist(playlistName: String): String {
        // Use .first() to get one emission from the Flow without hanging forever
        val existingPlaylist = try {
            musicRepository.getAllPlaylists().first().firstOrNull {
                it.name.equals(playlistName, ignoreCase = true)
            }?.id
        } catch (e: Exception) {
            null
        }

        if (existingPlaylist != null) {
            Log.d("DownloadManager", "Using existing playlist: $playlistName (id=$existingPlaylist)")
            return existingPlaylist
        }

        Log.d("DownloadManager", "Creating new playlist: $playlistName")
        return musicRepository.createPlaylist(playlistName, "Downloaded songs")
    }

    private fun parseDuration(durationStr: String): Long {
        return try {
            val parts = durationStr.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toLongOrNull() ?: 0
                val seconds = parts[1].toLongOrNull() ?: 0
                minutes * 60 + seconds
            } else if (parts.size == 3) {
                val hours = parts[0].toLongOrNull() ?: 0
                val minutes = parts[1].toLongOrNull() ?: 0
                val seconds = parts[2].toLongOrNull() ?: 0
                hours * 3600 + minutes * 60 + seconds
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w("DownloadManager", "Failed to parse duration: $durationStr")
            0
        }
    }

    private fun startDownloadService() {
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopDownloadService() {
        val intent = Intent(context, DownloadForegroundService::class.java).apply {
            action = DownloadForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun resetDownloadStatus(id: String) {
        val statusMap = _downloadStatus.value.toMutableMap()
        statusMap.remove(id)
        _downloadStatus.value = statusMap

        val progressMap = _downloadProgress.value.toMutableMap()
        progressMap.remove(id)
        _downloadProgress.value = progressMap

        val errorMap = _downloadErrors.value.toMutableMap()
        errorMap.remove(id)
        _downloadErrors.value = errorMap
    }
}
