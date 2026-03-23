package com.musictube.player.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.DownloadStatus
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.service.OkHttpDownloader
import com.musictube.player.service.YouTubeStreamService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val youTubeStreamService: YouTubeStreamService,
    private val downloader: OkHttpDownloader
) : ViewModel() {

    private val offlinePlaylistName = "Offline Downloads"
    private val favoritesPlaylistName = "Favorites"

    private val playlistId: String = savedStateHandle.get<String>("playlistId") ?: ""

    val playlist: StateFlow<Playlist?> =
        if (playlistId.isBlank()) {
            flowOf(null)
        } else {
            repository.getPlaylistByIdFlow(playlistId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val songs: StateFlow<List<Song>> =
        if (playlistId.isBlank()) {
            flowOf(emptyList())
        } else {
            repository.getPlaylistSongs(playlistId)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val allPlaylists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
        .map { playlists -> playlists.filterNot { it.id == playlistId } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val isShuffleOn: StateFlow<Boolean> = playerManager.isShuffleOn
    val isRepeatOn: StateFlow<Boolean> = playerManager.isRepeatOn

    fun toggleShuffle() = playerManager.toggleShuffle()
    fun toggleRepeat() = playerManager.toggleRepeat()

    fun playSong(song: Song) {
        val allSongs = songs.value
        val index = allSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerManager.setPlaylistQueue(allSongs, index)
        viewModelScope.launch {
            repository.incrementPlayCount(song.id)
        }
    }

    fun playPlaylistFromStart() {
        val allSongs = songs.value
        if (allSongs.isEmpty()) return
        val startIndex = if (playerManager.isShuffleOn.value) allSongs.indices.random() else 0
        playerManager.setPlaylistQueue(allSongs, startIndex)
    }

    fun removeSong(songId: String) {
        if (playlistId.isBlank()) return
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            _message.value = if (playlist.value?.name == offlinePlaylistName) {
                "Removed from Offline"
            } else {
                "Removed from playlist"
            }
        }
    }

    fun setLiked(song: Song, isLiked: Boolean) {
        viewModelScope.launch {
            repository.updateLikedStatus(song.id, isLiked)
            if (isLiked) {
                val favoritesId = getOrCreateFavoritesPlaylistId()
                repository.addSongToPlaylist(favoritesId, song.copy(isLiked = true))
                _message.value = "Added to Favorites"
            } else {
                val favoritesId = findFavoritesPlaylistId()
                if (favoritesId != null) {
                    repository.removeSongFromPlaylist(favoritesId, song.id)
                }
                _message.value = "Removed from Favorites"
            }
        }
    }

    private suspend fun getOrCreateFavoritesPlaylistId(): String {
        val existing = findFavoritesPlaylistId()
        if (existing != null) return existing
        return repository.createPlaylist(favoritesPlaylistName, "Songs you liked")
    }

    private fun findFavoritesPlaylistId(): String? {
        val current = playlist.value
        if (current != null && current.name.equals(favoritesPlaylistName, ignoreCase = true)) {
            return current.id
        }

        return allPlaylists.value.firstOrNull {
            it.name.equals(favoritesPlaylistName, ignoreCase = true)
        }?.id
    }

    fun addSongToPlaylist(song: Song, targetPlaylistId: String) {
        viewModelScope.launch {
            val added = repository.addExistingSongToPlaylist(targetPlaylistId, song.id)
            _message.value = if (added) {
                "Successfully added to the playlist"
            } else {
                "Song already exists in that playlist"
            }
        }
    }

    fun createPlaylistAndAdd(song: Song, name: String) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isBlank()) {
                _message.value = "Playlist name is required"
                return@launch
            }

            val existing = allPlaylists.value.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            if (existing != null) {
                _message.value = "Playlist already exists"
                return@launch
            }

            val newPlaylistId = repository.createPlaylist(trimmed)
            val added = repository.addExistingSongToPlaylist(newPlaylistId, song.id)
            _message.value = if (added) {
                "Successfully added to the playlist"
            } else {
                "Song already exists in that playlist"
            }
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun downloadSong(song: Song) {
        if (playlistId.isBlank() || song.isDownloaded) return

        viewModelScope.launch {
            val status = _downloadStatus.value.toMutableMap()
            status[song.id] = DownloadStatus.DOWNLOADING
            _downloadStatus.value = status

            val startProgress = _downloadProgress.value.toMutableMap()
            startProgress[song.id] = 0
            _downloadProgress.value = startProgress

            try {
                val videoId = song.id.removePrefix("yt_").removePrefix("dl_")
                val audioUrl = youTubeStreamService.extractAudioUrl(videoId)
                    ?: throw IllegalStateException("Unable to fetch downloadable stream")

                val localPath = downloader.downloadAudio(
                    url = audioUrl,
                    fileNameHint = "${song.title}_${song.artist}"
                ) { progress ->
                    val map = _downloadProgress.value.toMutableMap()
                    map[song.id] = progress
                    _downloadProgress.value = map
                }

                repository.insertSong(
                    song.copy(
                        filePath = localPath,
                        url = audioUrl,
                        isLocal = true,
                        isDownloaded = true
                    )
                )

                val done = _downloadStatus.value.toMutableMap()
                done[song.id] = DownloadStatus.COMPLETED
                _downloadStatus.value = done

                val doneProgress = _downloadProgress.value.toMutableMap()
                doneProgress[song.id] = 100
                _downloadProgress.value = doneProgress
            } catch (_: Exception) {
                val failed = _downloadStatus.value.toMutableMap()
                failed[song.id] = DownloadStatus.FAILED
                _downloadStatus.value = failed
            }
        }
    }
}
