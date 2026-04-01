package com.musictube.player.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.platform.DownloadController
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
import com.musictube.player.service.DownloadStatus as DlStatus

class AndroidDownloadManager(
    private val context: Context,
    private val downloader: OkHttpDownloader,
    private val youTubeStreamService: YouTubeStreamService,
    private val searchService: SearchService,
    private val musicRepository: MusicRepository
) : DownloadController {

    private val activeDownloadCount = AtomicInteger(0)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _downloadStatus = MutableStateFlow<Map<String, com.musictube.player.service.DownloadStatus>>(emptyMap())
    override val downloadStatus: StateFlow<Map<String, com.musictube.player.service.DownloadStatus>>
        get() = _downloadStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val _downloadErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    override val downloadErrors: StateFlow<Map<String, String>> = _downloadErrors.asStateFlow()

    private val _downloadQueue = MutableStateFlow<Map<String, SearchResult>>(emptyMap())
    override val downloadQueue: StateFlow<Map<String, SearchResult>> = _downloadQueue.asStateFlow()

    override fun downloadSong(searchResult: SearchResult, playlistName: String) {
        if (!searchResult.isPlayable) {
            Log.w("DownloadManager", "Attempted to download non-playable item: ${searchResult.id}")
            return
        }
        val existingStatus = _downloadStatus.value[searchResult.id]
        if (existingStatus == DlStatus.DOWNLOADING || existingStatus == DlStatus.COMPLETED) {
            Log.i("DownloadManager", "Skipping duplicate download for ${searchResult.id}")
            return
        }
        _downloadQueue.value = _downloadQueue.value + (searchResult.id to searchResult)

        val count = activeDownloadCount.incrementAndGet()
        if (count == 1) startDownloadService()

        scope.launch {
            _downloadStatus.value = _downloadStatus.value + (searchResult.id to DlStatus.DOWNLOADING)
            _downloadProgress.value = _downloadProgress.value + (searchResult.id to 0)
            _downloadErrors.value = _downloadErrors.value - searchResult.id

            try {
                val playlistId = getOrCreatePlaylist(playlistName)

                var audioUrl: String? = searchResult.audioUrl
                if (audioUrl == null) {
                    for (attempt in 1..3) {
                        audioUrl = extractBestAudioUrl(searchResult)
                        if (audioUrl != null) break
                        if (attempt < 3) delay(attempt * 3000L)
                    }
                }
                if (audioUrl == null) throw IllegalStateException(
                    "Could not extract downloadable audio URL after retries.")

                val localPath = downloader.downloadAudio(
                    url = audioUrl,
                    fileNameHint = "${searchResult.title}_${searchResult.artist}"
                ) { progress ->
                    _downloadProgress.value = _downloadProgress.value + (searchResult.id to progress)
                }

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
                _downloadStatus.value = _downloadStatus.value + (searchResult.id to DlStatus.COMPLETED)
                _downloadProgress.value = _downloadProgress.value + (searchResult.id to 100)
                Log.i("DownloadManager", "Successfully downloaded: ${searchResult.title}")
            } catch (e: Exception) {
                Log.e("DownloadManager", "Download failed for ${searchResult.id}: ${e.message}", e)
                _downloadStatus.value = _downloadStatus.value + (searchResult.id to DlStatus.FAILED)
                _downloadErrors.value = _downloadErrors.value + (searchResult.id to (e.message ?: "Unknown error"))
            } finally {
                val remaining = activeDownloadCount.decrementAndGet()
                if (remaining <= 0) { activeDownloadCount.set(0); stopDownloadService() }
            }
        }
    }

    private suspend fun extractBestAudioUrl(searchResult: SearchResult): String? {
        return withContext(Dispatchers.IO) {
            val primary = youTubeStreamService.extractAudioUrl(searchResult.id)
            if (primary != null) return@withContext primary
            val query = if (searchResult.artist.isNotBlank()) "${searchResult.artist} ${searchResult.title}"
                        else "${searchResult.title} official audio"
            val artistLower = searchResult.artist.trim().lowercase()
            val alternates = searchService.searchMusic(query, songsOnly = true)
                .asSequence()
                .filter { it.isPlayable && it.id.isNotBlank() && it.id != searchResult.id }
                .filter { c -> artistLower.isEmpty() || c.artist.trim().lowercase().contains(artistLower) || artistLower.contains(c.artist.trim().lowercase()) }
                .distinctBy { it.id }.take(8).toList()
            for (candidate in alternates) {
                val alt = youTubeStreamService.extractAudioUrl(candidate.id)
                if (alt != null) return@withContext alt
            }
            null
        }
    }

    private suspend fun getOrCreatePlaylist(playlistName: String): String {
        val existing = try {
            musicRepository.getAllPlaylists().first().firstOrNull { it.name.equals(playlistName, ignoreCase = true) }?.id
        } catch (e: Exception) { null }
        if (existing != null) return existing
        return musicRepository.createPlaylist(playlistName, "Downloaded songs")
    }

    private fun parseDuration(durationStr: String): Long = try {
        val parts = durationStr.split(":")
        when (parts.size) {
            2 -> (parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)
            3 -> (parts[0].toLongOrNull() ?: 0) * 3600 + (parts[1].toLongOrNull() ?: 0) * 60 + (parts[2].toLongOrNull() ?: 0)
            else -> 0
        }
    } catch (e: Exception) { 0 }

    private fun startDownloadService() {
        val intent = Intent(context, DownloadForegroundService::class.java).apply { action = DownloadForegroundService.ACTION_START }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    private fun stopDownloadService() {
        context.startService(Intent(context, DownloadForegroundService::class.java).apply { action = DownloadForegroundService.ACTION_STOP })
    }

    override fun resetDownloadStatus(id: String) {
        _downloadStatus.value = _downloadStatus.value - id
        _downloadProgress.value = _downloadProgress.value - id
        _downloadErrors.value = _downloadErrors.value - id
    }
}
