package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.platform.AudioPlayerController
import com.musictube.player.platform.DownloadController
import com.musictube.player.platform.platformDeleteFile
import com.musictube.player.service.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val playlistId: String,
    private val musicRepository: MusicRepository,
    private val playerController: AudioPlayerController,
    private val downloadController: DownloadController
) : ViewModel() {

    val playlist: StateFlow<Playlist?> = musicRepository.getPlaylistByIdFlow(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val songs: StateFlow<List<Song>> = musicRepository.getPlaylistSongs(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val downloadStatus   : StateFlow<Map<String, DownloadStatus>> = downloadController.downloadStatus
    val downloadProgress : StateFlow<Map<String, Int>>            = downloadController.downloadProgress

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // Expose player state for the mini-player bar
    val currentSong      = playerController.currentSong
    val isPlaying        = playerController.isPlaying
    val isShuffleOn      = playerController.isShuffleOn
    val isRepeatOn       = playerController.isRepeatOn
    val currentPosition  = playerController.currentPosition
    val duration         = playerController.duration
    val playQueueSize    = playerController.playQueueSize

    fun consumeMessage() { _message.value = null }

    fun pause()          { playerController.pause() }
    fun resume()         { playerController.resume() }
    fun playNext()       { playerController.playNext() }
    fun toggleShuffle()  { playerController.toggleShuffle() }
    fun toggleRepeat()   { playerController.toggleRepeat() }

    fun removeSelectedSongs(songIds: Set<String>) {
        viewModelScope.launch {
            songs.value.filter { it.id in songIds }.forEach { song ->
                musicRepository.removeSongFromPlaylist(playlistId, song.id)
                if (song.isDownloaded) {
                    musicRepository.markSongNotDownloaded(song.id)
                    deleteLocalFile(song.filePath)
                }
            }
        }
    }

    fun playSong(song: Song) {
        val queue = songs.value
        val idx   = queue.indexOfFirst { it.id == song.id }
        if (idx >= 0) playerController.setPlaylistQueue(queue, idx)
        else playerController.playSong(song)
    }

    fun playPlaylistFromStart() {
        val queue = songs.value
        if (queue.isNotEmpty()) playerController.setPlaylistQueue(queue, 0)
    }

    fun removeSong(song: Song) {
        viewModelScope.launch {
            musicRepository.removeSongFromPlaylist(playlistId, song.id)
            if (song.isDownloaded) {
                musicRepository.markSongNotDownloaded(song.id)
                deleteLocalFile(song.filePath)
            }
        }
    }

    fun setLiked(song: Song, liked: Boolean) {
        viewModelScope.launch {
            musicRepository.updateLikedStatus(song.id, liked)
            if (liked) {
                val favId = musicRepository.createPlaylist("Favorites")
                musicRepository.addExistingSongToPlaylist(favId, song.id)
            }
        }
    }

    fun addSongToPlaylist(targetPlaylistId: String, song: Song) {
        viewModelScope.launch { musicRepository.addExistingSongToPlaylist(targetPlaylistId, song.id) }
    }

    fun addSelectedSongsToPlaylist(songIds: Set<String>, targetPlaylistId: String) {
        viewModelScope.launch {
            songIds.forEach { songId -> musicRepository.addExistingSongToPlaylist(targetPlaylistId, songId) }
        }
    }

    fun createPlaylistAndAddSelected(songIds: Set<String>, name: String) {
        viewModelScope.launch {
            val id = musicRepository.createPlaylist(name)
            songIds.forEach { songId -> musicRepository.addExistingSongToPlaylist(id, songId) }
        }
    }

    fun createPlaylistAndAdd(name: String, song: Song) {
        viewModelScope.launch {
            val id = musicRepository.createPlaylist(name)
            musicRepository.addExistingSongToPlaylist(id, song.id)
        }
    }

    fun downloadSong(song: Song) {
        val result = SearchResult(
            id = song.id.removePrefix("yt_"),
            title = song.title, artist = song.artist,
            duration = "", thumbnailUrl = song.thumbnailUrl ?: "",
            videoUrl = song.url ?: ""
        )
        downloadController.downloadSong(result, playlist.value?.name ?: "Offline Downloads")
    }

    /** Platform must provide file deletion; we only clear the DB record here. */
    private fun deleteLocalFile(path: String?) {
        if (path == null) return
        // Actual file deletion happens via expect/actual in androidMain / iosMain
        platformDeleteFile(path)
    }
}


