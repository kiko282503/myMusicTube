package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.platform.AudioPlayerController
import com.musictube.player.platform.DownloadController
import com.musictube.player.service.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val playerController: AudioPlayerController,
    private val musicRepository: MusicRepository,
    private val downloadController: DownloadController
) : ViewModel() {

    val currentSong: StateFlow<Song?>  = playerController.currentSong
    val currentVideoId: StateFlow<String?> = playerController.currentVideoId
    val isPlaying: StateFlow<Boolean>  = playerController.isPlaying
    val isLoadingStream: StateFlow<Boolean> = playerController.isLoadingStream
    val currentPosition: StateFlow<Long> = playerController.currentPosition
    val duration: StateFlow<Long>       = playerController.duration
    val volume: StateFlow<Float>        = playerController.volume
    val hasPrevious: StateFlow<Boolean> = playerController.hasPrevious
    val hasNext: StateFlow<Boolean>     = playerController.hasNext
    val isShuffleOn: StateFlow<Boolean> = playerController.isShuffleOn
    val isRepeatOn: StateFlow<Boolean>  = playerController.isRepeatOn
    val playQueueSize: StateFlow<Int>   = playerController.playQueueSize

    val downloadStatus: StateFlow<Map<String, DownloadStatus>>  = downloadController.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>>           = downloadController.downloadProgress

    private val _currentDownloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.IDLE)
    val currentDownloadStatus: StateFlow<DownloadStatus> = _currentDownloadStatus.asStateFlow()

    private val _currentDownloadProgress = MutableStateFlow(0)
    val currentDownloadProgress: StateFlow<Int> = _currentDownloadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    val isCurrentSongInAnyPlaylist: StateFlow<Boolean> = currentSong
        .flatMapLatest { song ->
            if (song == null) flowOf(false) else musicRepository.isSongInAnyPlaylist(song.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
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

    fun play()                  = playerController.replayCurrent()
    fun pause()                 = playerController.pause()
    fun resume()                = playerController.resume()
    fun stop()                  = playerController.stop()
    fun seekTo(pos: Long)       = playerController.seekTo(pos)
    fun setVolume(v: Float)     = playerController.setVolume(v)
    fun playNext()              = playerController.playNext()
    fun playPrevious()          = playerController.playPrevious()
    fun toggleShuffle()         = playerController.toggleShuffle()
    fun toggleRepeat()          = playerController.toggleRepeat()

    fun downloadCurrentSong() {
        val song    = currentSong.value ?: return
        val videoId = currentVideoId.value ?: return
        val result  = com.musictube.player.data.model.SearchResult(
            id           = videoId,
            title        = song.title,
            artist       = song.artist,
            duration     = "${song.duration / 1000 / 60}:${(song.duration / 1000) % 60}",
            thumbnailUrl = song.thumbnailUrl ?: "",
            videoUrl     = song.url ?: "",
            audioUrl     = null,
            itemType     = "song"
        )
        _currentDownloadStatus.value = DownloadStatus.DOWNLOADING
        downloadController.downloadSong(result)
    }

    fun addCurrentSongToPlaylist(playlistId: String) {
        val song = currentSong.value ?: return
        viewModelScope.launch { musicRepository.addExistingSongToPlaylist(playlistId, song.id) }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { musicRepository.createPlaylist(name) }
    }

    fun getPlaylistList() = musicRepository.getAllPlaylists()
}
