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
import com.musictube.player.service.SearchStateHolder
import com.musictube.player.service.YouTubeStreamService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchService: SearchService,
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val downloadManager: DownloadManager,
    private val searchStateHolder: SearchStateHolder,
    private val youTubeStreamService: YouTubeStreamService
) : ViewModel() {

    private val offlinePlaylistName = "Offline Downloads"

    private val _searchQuery = MutableStateFlow(searchStateHolder.lastQuery)
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow(searchStateHolder.lastResults)
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // False once loadMoreResults finds no new items - resets on each new search
    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    // Continuation token from the last search page — used to fetch the real next page
    private var continuationToken: String? = null

    // Expose player preview state so the search list can show play/pause per item
    val previewVideoId: StateFlow<String?> = playerManager.currentVideoId
    val previewIsPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val previewIsLoading: StateFlow<Boolean> = playerManager.isLoadingStream

    // Expose download status/progress/errors from DownloadManager
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadProgress
    val downloadErrors: StateFlow<Map<String, String>> = downloadManager.downloadErrors

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

    private var debounceJob: Job? = null
    private var searchJob: Job? = null

    init {
        // Restore pagination cursor so load-more works immediately after restore
        continuationToken = searchStateHolder.continuationToken
        _canLoadMore.value = searchStateHolder.canLoadMore
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchStateHolder.lastQuery = query
        debounceJob?.cancel()
        if (query.isNotEmpty()) {
            debounceJob = viewModelScope.launch {
                delay(400) // Wait 400ms after the user stops typing
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
        // Stop preview before wiping results (stopPreviewIfActive checks the results list)
        stopPreviewIfActive()
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _canLoadMore.value = true
        continuationToken = null
        searchStateHolder.lastQuery = ""
        searchStateHolder.lastResults = emptyList()
        searchStateHolder.continuationToken = null
        searchStateHolder.canLoadMore = true
    }

    /**
     * Stop audio only if the current song is a preview playing from the search screen
     * (i.e. not something the user explicitly navigated to the player for).
     */
    fun stopPreviewIfActive() {
        val currentId = playerManager.currentVideoId.value ?: return
        val isFromSearch = _searchResults.value.any { it.id == currentId }
        if (isFromSearch && (playerManager.isPlaying.value || playerManager.isLoadingStream.value)) {
            playerManager.pause()
        }
    }

    fun searchMusic() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (_searchQuery.value.isBlank()) return@launch

            // Stop any in-progress preview before loading new results
            stopPreviewIfActive()

            _isLoading.value = true
            _errorMessage.value = null
            _canLoadMore.value = true
            continuationToken = null

            try {
                val (results, token) = searchService.searchMusicPaged(_searchQuery.value, songsOnly = true)
                // If the query was cleared while this network call was in-flight, discard results
                if (_searchQuery.value.isBlank()) return@launch
                continuationToken = token
                val filtered = results
                    .filter { it.itemType !in setOf("episode", "podcast") }
                    .distinctBy { it.id }
                    .distinctBy { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }
                _searchResults.value = filtered
                _canLoadMore.value = token != null
                searchStateHolder.lastResults = filtered
                searchStateHolder.continuationToken = token
                searchStateHolder.canLoadMore = token != null

                if (filtered.isEmpty()) {
                    _canLoadMore.value = false
                    _errorMessage.value = "No results found for '${_searchQuery.value}'"
                } else {
                    // Prefetch audio URLs for the first 20 results in background so
                    // tapping play is near-instant (results land in the URL cache)
                    val toWarm = filtered.filter { it.isPlayable }.take(20)
                    viewModelScope.launch(Dispatchers.IO) {
                        toWarm.forEach { youTubeStreamService.prefetchAudioUrl(it.id) }
                    }
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

        // If this song is already playing or loading (e.g. via preview), just navigate
        // to the player without restarting — let it continue from where it is.
        val alreadyCurrent = playerManager.currentVideoId.value == searchResult.id
        val activeStream = playerManager.isPlaying.value || playerManager.isLoadingStream.value
        if (alreadyCurrent && activeStream) return

        playerManager.playYouTubeAudioStream(
            videoId = searchResult.id,
            title = searchResult.title,
            artist = searchResult.artist,
            thumbnailUrl = searchResult.thumbnailUrl
        )
    }

    /** Play or pause a preview of [searchResult] inline without navigating to the player page. */
    fun togglePreview(searchResult: SearchResult) {
        if (!searchResult.isPlayable) return
        when {
            playerManager.currentVideoId.value == searchResult.id && playerManager.isPlaying.value -> {
                playerManager.pause()
            }
            playerManager.currentVideoId.value == searchResult.id && !playerManager.isPlaying.value -> {
                playerManager.resume()
            }
            else -> {
                playerManager.playYouTubeAudioStream(
                    videoId = searchResult.id,
                    title = searchResult.title,
                    artist = searchResult.artist,
                    thumbnailUrl = searchResult.thumbnailUrl
                )
            }
        }
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
        val token = continuationToken ?: run {
            _canLoadMore.value = false
            return
        }

        _isLoadingMore.value = true

        viewModelScope.launch {
            try {
                val existingIds = _searchResults.value.map { it.id }.toMutableSet()
                val existingTitleArtist = _searchResults.value
                    .map { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" }
                    .toMutableSet()
                var currentToken: String = token
                var skips = 0
                val maxSkips = 10
                var foundFresh = false

                while (skips <= maxSkips) {
                    val (newResults, nextToken) = searchService.fetchContinuation(currentToken, songsOnly = true)

                    val fresh = newResults.filter {
                        it.id !in existingIds &&
                        it.itemType !in setOf("episode", "podcast") &&
                        "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" !in existingTitleArtist
                    }

                    if (fresh.isNotEmpty()) {
                        existingIds.addAll(fresh.map { it.id })
                        existingTitleArtist.addAll(fresh.map { "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}" })
                        val combined = _searchResults.value + fresh
                        _searchResults.value = combined
                        // Persist the expanded list
                        searchStateHolder.lastResults = combined
                        searchStateHolder.continuationToken = nextToken
                        searchStateHolder.canLoadMore = nextToken != null
                        // Save the next token for the subsequent load-more call
                        continuationToken = nextToken
                        _canLoadMore.value = nextToken != null
                        foundFresh = true
                        break
                    } else if (nextToken != null) {
                        // Page had no new items — advance to next continuation and retry
                        currentToken = nextToken
                        skips++
                    } else {
                        // Truly no more pages
                        continuationToken = null
                        _canLoadMore.value = false
                        break
                    }
                }

                if (!foundFresh && skips > maxSkips) {
                    // Hit skip limit but don't kill pagination permanently —
                    // save where we are so the next scroll can try again
                    continuationToken = currentToken
                    _canLoadMore.value = true
                }
            } catch (e: Exception) {
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
