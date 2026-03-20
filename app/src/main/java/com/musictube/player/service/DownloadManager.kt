package com.musictube.player.service

import android.util.Log
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val downloader: OkHttpDownloader,
    private val youTubeStreamService: YouTubeStreamService,
    private val searchService: SearchService,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val _downloadErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val downloadErrors: StateFlow<Map<String, String>> = _downloadErrors.asStateFlow()

    fun downloadSong(
        searchResult: SearchResult,
        playlistName: String = "Offline Downloads"
    ) {
        if (!searchResult.isPlayable) {
            Log.w("DownloadManager", "Attempted to download non-playable item: ${searchResult.id}")
            return
        }

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

                val audioUrl = searchResult.audioUrl
                    ?: extractBestAudioUrl(searchResult)
                    ?: throw IllegalStateException("Could not extract downloadable audio URL")

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

            // Fallback: broaden query and try alternates from fresh search.
            val query = when {
                searchResult.artist.isNotBlank() -> "${searchResult.title} ${searchResult.artist} official audio"
                else -> "${searchResult.title} official audio"
            }

            Log.d("DownloadManager", "Primary failed, broadening search with: $query")

            val alternates = searchService.searchMusic(query, songsOnly = false)
                .asSequence()
                .filter { it.isPlayable && it.id.isNotBlank() && it.id != searchResult.id }
                .distinctBy { it.id }
                .take(12)
                .toList()

            for (candidate in alternates) {
                val alt = youTubeStreamService.extractAudioUrl(candidate.id)
                if (alt != null) {
                    Log.d("DownloadManager", "Found alternate audio for ${searchResult.id} using ${candidate.id}")
                    return@withContext alt
                }
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
