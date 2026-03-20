package com.musictube.player.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.DownloadManager
import com.musictube.player.service.DownloadStatus
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.service.OkHttpDownloader
import com.musictube.player.service.SearchService
import com.musictube.player.service.YouTubeStreamService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchService: SearchService,
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val offlinePlaylistName = "Offline Downloads"

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // False once loadMoreResults finds no new items - resets on each new search
    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    // Expose download status/progress from DownloadManager
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadProgress

    // DB-backed set of YouTube video IDs that are already downloaded (survives app restarts)
    val downloadedVideoIds: StateFlow<Set<String>> = musicRepository.getDownloadedSongs()
        .map { songs ->
            songs.mapNotNull { song ->
                if (song.id.startsWith("yt_")) song.id.removePrefix("yt_") else null
            }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            _canLoadMore.value = true
            searchMusic()
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _canLoadMore.value = true
    }

    fun searchMusic() {
        viewModelScope.launch {
            if (_searchQuery.value.isBlank()) return@launch

            _isLoading.value = true
            _errorMessage.value = null
            _canLoadMore.value = true  // Reset load-more for each fresh search

            try {
                val results = searchService.searchMusic(_searchQuery.value)
                _searchResults.value = results

                if (results.isEmpty()) {
                    _canLoadMore.value = false
                    _errorMessage.value = "No results found for '${_searchQuery.value}'"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                _canLoadMore.value = false
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playSearchResult(searchResult: SearchResult) {
        if (!searchResult.isPlayable) {
            _errorMessage.value = "This item cannot be played directly"
            return
        }

        // Use YouTube WebView embed - no audio extraction needed
        playerManager.playYouTubeAudioStream(
            videoId = searchResult.id,
            title = searchResult.title,
            artist = searchResult.artist,
            thumbnailUrl = searchResult.thumbnailUrl
        )
    }

    fun addSongToSelectedPlaylist(searchResult: SearchResult) {
        viewModelScope.launch {
            try {
                val playlistId = getOrCreateOfflinePlaylist()
                val song = Song(
                    id = "yt_${searchResult.id}",
                    title = searchResult.title,
                    artist = searchResult.artist,
                    duration = parseDuration(searchResult.duration),
                    filePath = null,
                    url = searchResult.videoUrl,
                    thumbnailUrl = searchResult.thumbnailUrl,
                    isLocal = false,
                    isDownloaded = false
                )
                musicRepository.addSongToPlaylist(playlistId, song)
            } catch (e: Exception) {
                _errorMessage.value = "Add to playlist failed: ${e.message}"
            }
        }
    }

    fun downloadSong(searchResult: SearchResult) {
        downloadManager.downloadSong(searchResult, offlinePlaylistName)
    }

    fun loadMoreResults() {
        // Don't attempt if already loading or confirmed no more results exist
        if (_isLoadingMore.value || _searchQuery.value.isBlank() || !_canLoadMore.value) return

        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                // Fetch a fresh batch using a slight variation to try getting different items
                val baseQuery = _searchQuery.value
                val additionalResults = searchService.searchMusic(baseQuery)
                
                // Merge with existing results, avoiding duplicates
                val existingIds = _searchResults.value.map { it.id }.toSet()
                val newResults = additionalResults.filter { it.id !in existingIds }
                
                if (newResults.isNotEmpty()) {
                    _searchResults.value = _searchResults.value + newResults
                    Log.d("SearchViewModel", "Loaded ${newResults.size} more, total: ${_searchResults.value.size}")
                } else {
                    // No new results — stop triggering further loads
                    _canLoadMore.value = false
                    Log.d("SearchViewModel", "No new results, disabling load-more")
                }
            } catch (e: Exception) {
                _canLoadMore.value = false
                Log.e("SearchViewModel", "Failed to load more results: ${e.message}", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun selectPlaylist(playlistId: String) {
        _selectedPlaylistId.value = playlistId
    }

    fun createPlaylist(name: String, description: String? = null) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val id = musicRepository.createPlaylist(name.trim(), description)
            _selectedPlaylistId.value = id
        }
    }

    private suspend fun getOrCreateSelectedPlaylist(): String {
        val selected = _selectedPlaylistId.value
        if (selected != null) return selected

        val existing = playlists.value.firstOrNull()
        if (existing != null) {
            _selectedPlaylistId.value = existing.id
            return existing.id
        }

        val created = musicRepository.createPlaylist("Offline Downloads", "Downloaded songs")
        _selectedPlaylistId.value = created
        return created
    }

    private suspend fun getOrCreateOfflinePlaylist(): String {
        val existingOffline = playlists.value.firstOrNull {
            it.name.equals(offlinePlaylistName, ignoreCase = true)
        }
        if (existingOffline != null) {
            return existingOffline.id
        }

        return musicRepository.createPlaylist(offlinePlaylistName, "Downloaded songs")
    }

    private fun parseDuration(durationStr: String): Long {
        return try {
            val parts = durationStr.split(":")
            if (parts.size == 2) {
                val minutes = parts[0].toLongOrNull() ?: 0
                val seconds = parts[1].toLongOrNull() ?: 0
                (minutes * 60 + seconds) * 1000
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
