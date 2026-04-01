package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.SharedPlaylistData
import com.musictube.player.data.model.SharedSongData
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.platform.AudioPlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SharedPlaylistViewModel(
    private val playerController: AudioPlayerController,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _playlistData  = MutableStateFlow<SharedPlaylistData?>(null)
    val playlistData: StateFlow<SharedPlaylistData?> = _playlistData.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    val currentSong      = playerController.currentSong
    val isPlaying        = playerController.isPlaying
    val currentPosition  = playerController.currentPosition
    val duration         = playerController.duration

    val isPlayingFromHere: StateFlow<Boolean> = combine(
        playerController.currentVideoId, _playlistData
    ) { vid, data ->
        vid != null && data?.songs?.any { it.videoId == vid } == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPlaylistSongs(data: SharedPlaylistData) { _playlistData.value = data }

    fun consumeMessage() { _message.value = null }

    fun pause()  { playerController.pause() }
    fun resume() { playerController.resume() }

    fun playNow() {
        val data = _playlistData.value ?: return
        val queue = data.songs.map { it.toSong() }
        playerController.setPlaylistQueue(queue, 0)
    }

    fun playSongAt(index: Int) {
        val queue = _playlistData.value?.songs?.map { it.toSong() } ?: return
        playerController.setPlaylistQueue(queue, index)
    }

    fun saveAsPlaylist(name: String) {
        val data = _playlistData.value ?: return
        viewModelScope.launch {
            val playlistId = musicRepository.createPlaylist(name)
            data.songs.forEach { shared ->
                val song = shared.toSong()
                musicRepository.addSongToPlaylist(playlistId, song)
            }
            _isSaved.value = true
            _message.value = "Playlist \"$name\" saved!"
        }
    }

    private fun SharedSongData.toSong() = Song(
        id = videoId, title = title, artist = artist, isLocal = false,
        url = "https://music.youtube.com/watch?v=$videoId"
    )
}
