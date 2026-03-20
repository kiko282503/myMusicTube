package com.musictube.player.viewmodel

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.DownloadManager
import com.musictube.player.service.DownloadStatus
import com.musictube.player.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: MusicPlayerManager,
    private val musicRepository: MusicRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val currentVideoId: StateFlow<String?> = playerManager.currentVideoId
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val volume: StateFlow<Float> = playerManager.volume

    // Expose download status/progress from DownloadManager
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadProgress

    // Local download state for current song (separate from DownloadManager which handles SearchResults)
    private val _currentDownloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.IDLE)
    val currentDownloadStatus: StateFlow<DownloadStatus> = _currentDownloadStatus.asStateFlow()

    private val _currentDownloadProgress = MutableStateFlow(0)
    val currentDownloadProgress: StateFlow<Int> = _currentDownloadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    /** Emits true when the current song is already present in any playlist. */
    val isCurrentSongInAnyPlaylist: StateFlow<Boolean> = currentSong
        .flatMapLatest { song ->
            if (song == null) flowOf(false)
            else musicRepository.isSongInAnyPlaylist(song.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Sync download-status indicator whenever the active song changes.
        // We always check the DB because the transient Song object created by
        // playYouTubeAudioStream() has isDownloaded = false by default.
        viewModelScope.launch {
            currentSong.collect { song ->
                if (song != null) {
                    val dbSong = musicRepository.getSongById(song.id)
                    _currentDownloadStatus.value =
                        if (song.isDownloaded || dbSong?.isDownloaded == true) DownloadStatus.COMPLETED
                        else DownloadStatus.IDLE
                }
            }
        }
    }

    /** Returns the singleton WebView from MusicPlayerManager — creates it on first call. */
    fun getOrCreateWebView(context: Context): WebView = playerManager.getOrCreateWebView(context)

    /** Call when the app comes back to foreground so YouTube Music resumes if paused. */
    fun resumeWebView() = playerManager.resumeWebView()

    /** Park the WebView in the decor view so audio keeps playing when navigating back. */
    fun parkWebView(context: Context) = playerManager.parkWebView(context)

    fun play() { playerManager.replayCurrent() }
    fun pause() { playerManager.pause() }
    fun resume() { playerManager.resume() }
    fun stop() { playerManager.stop() }
    fun seekTo(position: Long) { playerManager.seekTo(position) }
    fun setVolume(volume: Float) { playerManager.setVolume(volume) }

    fun downloadCurrentSong() {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            _currentDownloadStatus.value = DownloadStatus.DOWNLOADING
            _currentDownloadProgress.value = 0

            try {
                val videoId = currentVideoId.value
                    ?: throw IllegalStateException("No video ID available")

                // Create a SearchResult-like object to pass to DownloadManager
                val searchResult = com.musictube.player.data.model.SearchResult(
                    id = videoId,
                    title = song.title,
                    artist = song.artist,
                    duration = "${song.duration / 1000 / 60}:${(song.duration / 1000) % 60}",
                    thumbnailUrl = song.thumbnailUrl ?: "",
                    videoUrl = song.url ?: "",
                    audioUrl = null,
                    itemType = "song",
                    isPlayable = true
                )

                downloadManager.downloadSong(searchResult, "Offline Downloads")
                
                // Wait for the download to complete by observing the status
                var attempts = 0
                while (_currentDownloadStatus.value == DownloadStatus.DOWNLOADING && attempts < 300) {
                    val status = downloadManager.downloadStatus.value[videoId]
                    if (status == DownloadStatus.COMPLETED || status == DownloadStatus.FAILED) {
                        _currentDownloadStatus.value = status ?: DownloadStatus.FAILED
                        _currentDownloadProgress.value = downloadManager.downloadProgress.value[videoId] ?: 0
                        break
                    }
                    _currentDownloadProgress.value = downloadManager.downloadProgress.value[videoId] ?: 0
                    kotlinx.coroutines.delay(100)
                    attempts++
                }
            } catch (e: Exception) {
                _currentDownloadStatus.value = DownloadStatus.FAILED
                _errorMessage.value = "Download failed: ${e.message}"
            }
        }
    }

    fun addCurrentSongToPlaylist(playlistId: String) {
        val song = currentSong.value ?: return
        viewModelScope.launch {
            try {
                musicRepository.addSongToPlaylist(playlistId, song)
                _errorMessage.value = "Added to playlist"
            } catch (e: Exception) {
                _errorMessage.value = "Add to playlist failed: ${e.message}"
            }
        }
    }

    fun getPlaylistList() = musicRepository.getAllPlaylists()

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                musicRepository.createPlaylist(name.trim(), description)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't release the player — it should continue playing in background
    }
}