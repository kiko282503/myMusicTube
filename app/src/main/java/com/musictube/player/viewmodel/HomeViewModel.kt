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
import com.musictube.player.service.SearchService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val searchService: SearchService,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val offlinePlaylistName = "Offline Downloads"

    private val allTrendingQueries = listOf(
        "trending music 2026", "viral hits 2026", "chart toppers 2026",
        "new music releases 2026", "popular songs 2026", "top hits 2026",
        "best songs 2026", "hit music 2026", "latest songs 2026",
        "music trends 2026", "hot songs 2026", "trending now 2026",
        "popular tracks 2026", "viral music 2026", "current hits 2026"
    )
    private var trendingPage = 0
    
    private fun getRandomTrendingQuery(): String {
        return allTrendingQueries.random()
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _trendingSongs = MutableStateFlow<List<SearchResult>>(emptyList())
    val trendingSongs: StateFlow<List<SearchResult>> = _trendingSongs.asStateFlow()

    private val _quickPicks = MutableStateFlow<List<SearchResult>>(emptyList())
    val quickPicks: StateFlow<List<SearchResult>> = _quickPicks.asStateFlow()

    // Expose download status/progress from DownloadManager
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadProgress

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val offlinePlaylist: StateFlow<Playlist?> = playlists
        .map { list -> list.firstOrNull { it.name == offlinePlaylistName } ?: list.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val offlinePlaylistId: StateFlow<String?> = offlinePlaylist
        .map { it?.id }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val offlineSongCount: StateFlow<Int> = offlinePlaylist
        .map { it?.songCount ?: 0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val songs: StateFlow<List<Song>> = musicRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptyList()
        )

    // Expose current playback state so HomeScreen can show a Now Playing bar.
    val currentSong = playerManager.currentSong
    val isPlayingNow = playerManager.isPlaying

    init {
        ensureOfflinePlaylist()
        loadTrendingSongs()
        loadQuickPicks()
    }

    private fun ensureOfflinePlaylist() {
        viewModelScope.launch {
            val exists = playlists.value.any { it.name == offlinePlaylistName }
            if (!exists) {
                musicRepository.createPlaylist(offlinePlaylistName, "Downloaded songs")
            }
        }
    }

    private fun loadTrendingSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val songOnlyResults = searchService.searchMusic(getRandomTrendingQuery(), songsOnly = true)
                val results = if (songOnlyResults.isNotEmpty()) {
                    songOnlyResults
                } else {
                    searchService.searchMusic(getRandomTrendingQuery(), songsOnly = false)
                }
                _trendingSongs.value = results.distinctBy { it.id }
                trendingPage = 1
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load trending songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMoreTrending() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                trendingPage++
                val existingIds = _trendingSongs.value.asSequence().map { it.id }.toHashSet()
                val merged = _trendingSongs.value.toMutableList()

                repeat(3) {
                    val query = getRandomTrendingQuery()
                    val songOnlyResults = searchService.searchMusic(query, songsOnly = true)
                    val results = if (songOnlyResults.isNotEmpty()) {
                        songOnlyResults
                    } else {
                        searchService.searchMusic(query, songsOnly = false)
                    }
                    val fresh = results.filter { it.id !in existingIds }
                    if (fresh.isNotEmpty()) {
                        merged.addAll(fresh)
                        fresh.forEach { existingIds.add(it.id) }
                    }
                }

                _trendingSongs.value = merged
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load more trending", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private fun loadQuickPicks() {
        viewModelScope.launch {
            try {
                // Get YouTube search results only - no sample songs in production
                val youtubResults = searchService.searchMusic(getRandomTrendingQuery(), songsOnly = true)
                
                // Take the first 6 results for Quick Picks
                _quickPicks.value = youtubResults.take(6)
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load quick picks", e)
                
                // Fallback: Empty list if search fails
                _quickPicks.value = emptyList()
            }
        }
    }

    fun reloadTrending() {
        trendingPage = 0
        _trendingSongs.value = emptyList()
        loadTrendingSongs()
        loadQuickPicks()
    }

    fun playSearchResult(searchResult: SearchResult) {
        if (!searchResult.isPlayable) return

        // All search results are YouTube tracks - use stream extraction
        playerManager.playYouTubeAudioStream(
            videoId = searchResult.id,
            title = searchResult.title,
            artist = searchResult.artist,
            thumbnailUrl = searchResult.thumbnailUrl
        )
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            playerManager.playSong(song)
            musicRepository.incrementPlayCount(song.id)
        }
    }

    fun downloadSong(searchResult: SearchResult) {
        downloadManager.downloadSong(searchResult, offlinePlaylistName)
    }
}