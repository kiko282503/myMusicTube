package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.SharedSongData
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedPlaylistViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    fun playNow(songs: List<SharedSongData>) {
        val songObjects = songs.map { it.toSong() }
        playerManager.setPlaylistQueue(songObjects, 0)
    }

    fun saveAsPlaylist(name: String, songs: List<SharedSongData>) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) { _message.value = "Playlist name is required"; return@launch }
            val playlistId = repository.createPlaylist(trimmed)
            var added = 0
            songs.forEach { shared ->
                if (repository.addSongToPlaylist(playlistId, shared.toSong())) added++
            }
            _message.value = "\"$trimmed\" saved with $added song(s)"
            _isSaved.value = true
        }
    }

    fun consumeMessage() { _message.value = null }

    private fun SharedSongData.toSong() = Song(
        id = "yt_$videoId",
        title = title,
        artist = artist,
        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        isLocal = false,
        isDownloaded = false
    )
}
