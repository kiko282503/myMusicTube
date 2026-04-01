package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.platform.AudioPlayerController
import com.musictube.player.platform.DownloadController
import com.musictube.player.service.DownloadStatus
import com.musictube.player.service.SearchService
import com.musictube.player.service.SearchStateHolder
import com.musictube.player.service.YouTubeStreamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    private val searchService: SearchService,
    private val musicRepository: MusicRepository,
    private val playerController: AudioPlayerController,
    private val downloadController: DownloadController,
    private val searchStateHolder: SearchStateHolder,
    private val youTubeStreamService: YouTubeStreamService
) : ViewModel() {

    private val offlinePlaylistName = "Offline Downloads"

    private val _searchQuery   = MutableStateFlow(searchStateHolder.lastQuery)
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(searchStateHolder.lastResults)
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isLoading     = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore   = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private var continuationToken: String? = null

    val previewVideoId  : StateFlow<String?> = playerController.currentVideoId
    val previewIsPlaying: StateFlow<Boolean> = playerController.isPlaying
    val previewIsLoading: StateFlow<Boolean> = playerController.isLoadingStream

    val downloadStatus  : StateFlow<Map<String, DownloadStatus>> = downloadController.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>>            = downloadController.downloadProgress
    val downloadErrors  : StateFlow<Map<String, String>>         = downloadController.downloadErrors

    val downloadedVideoIds: StateFlow<Set<String>> = musicRepository.getDownloadedSongs()
        .map { songs -> songs.mapNotNull { if (it.id.startsWith("yt_")) it.id.removePrefix("yt_") else null }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var debounceJob: Job? = null
    private var searchJob  : Job? = null

    init {
        continuationToken    = searchStateHolder.continuationToken
        _canLoadMore.value   = searchStateHolder.canLoadMore
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchStateHolder.lastQuery = query
        debounceJob?.cancel()
        if (query.isNotEmpty()) {
            debounceJob = viewModelScope.launch {
                delay(400)
                _canLoadMore.value = true
                searchMusic()
            }
        } else {
            searchJob?.cancel()
            _searchResults.value = emptyList()
            searchStateHolder.lastResults = emptyList()
        }
    }

    fun clearSearch() {
        stopPreviewIfActive()
        _searchQuery.value    = ""
        _searchResults.value  = emptyList()
        _canLoadMore.value    = true
        continuationToken     = null
        searchStateHolder.lastQuery         = ""
        searchStateHolder.lastResults       = emptyList()
        searchStateHolder.continuationToken = null
        searchStateHolder.canLoadMore       = true
    }

    fun stopPreviewIfActive() {
        val currentId  = playerController.currentVideoId.value ?: return
        val fromSearch = _searchResults.value.any { it.id == currentId }
        if (fromSearch && (playerController.isPlaying.value || playerController.isLoadingStream.value)) {
            playerController.pause()
        }
    }

    fun searchMusic() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (_searchQuery.value.isBlank()) return@launch
            stopPreviewIfActive()
            _isLoading.value    = true
            _errorMessage.value = null
            _canLoadMore.value  = true
            continuationToken   = null

            try {
                val (results, token) = searchService.searchMusicPaged(_searchQuery.value, songsOnly = true)
                if (_searchQuery.value.isBlank()) return@launch
                continuationToken = token
                val filtered = results
                    .filter { it.itemType !in setOf("episode", "podcast") }
                    .distinctBy { it.id }
                    .distinctBy { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }
                _searchResults.value         = filtered
                _canLoadMore.value           = token != null
                searchStateHolder.lastResults       = filtered
                searchStateHolder.continuationToken = token
                searchStateHolder.canLoadMore       = token != null
                if (filtered.isEmpty()) {
                    _canLoadMore.value   = false
                    _errorMessage.value = "No results found for '${_searchQuery.value}'"
                } else {
                    viewModelScope.launch(Dispatchers.Default) {
                        filtered.filter { it.isPlayable }.take(20)
                            .forEach { youTubeStreamService.prefetchAudioUrl(it.id) }
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value  = "Search failed: ${e.message}"
                _canLoadMore.value   = false
                _searchResults.value = emptyList()
            } finally { _isLoading.value = false }
        }
    }

    fun playSearchResult(searchResult: SearchResult) {
        if (!searchResult.isPlayable) { _errorMessage.value = "This item cannot be played directly"; return }
        val alreadyPlayingThis = playerController.currentVideoId.value == searchResult.id
        if (alreadyPlayingThis && (playerController.isPlaying.value || playerController.isLoadingStream.value)) return
        val song = Song(
            id = searchResult.id, title = searchResult.title, artist = searchResult.artist,
            thumbnailUrl = searchResult.thumbnailUrl, isLocal = false
        )
        playerController.playYouTubeAudioStream(searchResult.id, song)
    }

    fun togglePreview(searchResult: SearchResult) {
        val currentId  = playerController.currentVideoId.value
        val isThisSong = currentId == searchResult.id
        when {
            isThisSong && playerController.isPlaying.value -> playerController.pause()
            isThisSong && !playerController.isPlaying.value -> playerController.resume()
            else -> playSearchResult(searchResult)
        }
    }

    fun downloadSong(searchResult: SearchResult) {
        viewModelScope.launch {
            val playlistId = musicRepository.createPlaylist(offlinePlaylistName)
            downloadController.downloadSong(searchResult, offlinePlaylistName)
        }
    }

    fun loadMoreResults() {
        val token = continuationToken
        if (token == null || !_canLoadMore.value || _isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            var attempts = 0
            var currentToken: String = token
            try {
                while (attempts < 10) {
                    val (newResults, nextToken) = searchService.fetchContinuation(currentToken, songsOnly = true)
                    val existing = _searchResults.value
                    val merged   = (existing + newResults).distinctBy { it.id }
                        .distinctBy { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }
                    if (merged.size > existing.size) {
                        _searchResults.value         = merged
                        searchStateHolder.lastResults       = merged
                        continuationToken                   = nextToken
                        searchStateHolder.continuationToken = nextToken
                        _canLoadMore.value                  = nextToken != null
                        searchStateHolder.canLoadMore       = nextToken != null
                        break
                    }
                    val nextNonNull = nextToken ?: break
                    currentToken = nextNonNull
                    attempts++
                }
                if (attempts >= 10) _canLoadMore.value = false
            } finally { _isLoadingMore.value = false }
        }
    }

    fun selectPlaylist(id: String?) { _selectedPlaylistId.value = id }

    fun createPlaylistAndDownload(name: String, searchResult: SearchResult) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name)
            downloadController.downloadSong(searchResult, name)
        }
    }
}
