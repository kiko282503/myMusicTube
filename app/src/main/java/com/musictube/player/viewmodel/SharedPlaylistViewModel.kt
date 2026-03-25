package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.SharedSongData
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    // Live playback state so the screen can show which song is active
    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration

    // IDs of songs belonging to the currently displayed shared playlist.
    // Kept in the ViewModel (not in screen's `remember`) so it survives
    // navigation to PlayerScreen and back without going stale.
    private val _playlistSongIds = MutableStateFlow<Set<String>>(emptySet())

    /** True when the currently playing song is from this shared playlist. */
    val isPlayingFromHere: StateFlow<Boolean> = combine(
        playerManager.currentSong,
        _playlistSongIds
    ) { song, ids ->
        song?.id != null && ids.isNotEmpty() && song.id in ids
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Called from the screen on every entry so the ViewModel knows which IDs
     * belong to this playlist. Safe to call repeatedly with the same data.
     */
    fun setPlaylistSongs(songs: List<SharedSongData>) {
        _playlistSongIds.value = songs.map { "yt_${it.videoId}" }.toSet()
    }

    fun playNow(songs: List<SharedSongData>) {
        val songObjects = songs.map { it.toSong() }
        _playlistSongIds.value = songObjects.map { it.id }.toSet()
        playerManager.setPlaylistQueue(songObjects, 0)
    }

    fun playSongAt(songs: List<SharedSongData>, index: Int) {
        val songObjects = songs.map { it.toSong() }
        _playlistSongIds.value = songObjects.map { it.id }.toSet()
        playerManager.setPlaylistQueue(songObjects, index)
    }

    fun pause() = playerManager.pause()
    fun resume() = playerManager.resume()

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
