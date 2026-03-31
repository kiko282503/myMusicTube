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
import com.musictube.player.service.YouTubeStreamService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val searchService: SearchService,
    private val downloadManager: DownloadManager,
    private val youTubeStreamService: YouTubeStreamService
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

    private val allQuickPickQueries = listOf(
        "classic rock hits", "feel good songs", "chill vibes playlist",
        "throwback hits", "acoustic songs", "indie music favorites",
        "road trip songs", "workout music", "relaxing music",
        "80s greatest hits", "90s pop hits", "soft rock classics",
        "jazz classics", "soul music favorites", "r&b hits"
    )

    private fun getRandomTrendingQuery(): String {
        return allTrendingQueries.random()
    }

    private fun getRandomQuickPickQuery(): String {
        return allQuickPickQueries.random()
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
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val playQueueSize: StateFlow<Int> = playerManager.playQueueSize

    // Preview state for inline play buttons on search result items
    val previewVideoId: StateFlow<String?> = playerManager.currentVideoId
    val previewIsPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val previewIsLoading: StateFlow<Boolean> = playerManager.isLoadingStream

    fun pause() = playerManager.pause()
    fun resume() = playerManager.resume()
    fun playNext() = playerManager.playNext()

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch {
            musicRepository.renamePlaylist(playlistId, newName.trim())
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            musicRepository.deletePlaylist(playlistId)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            musicRepository.createPlaylist(name.trim())
        }
    }

    init {
        ensureOfflinePlaylist()
        loadTrendingSongs() // quickPicks are derived inside after results arrive
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
                // Race home-feed browse API against a search query in parallel — show whichever responds first
                val trendingQuery = getRandomTrendingQuery()
                val results = channelFlow<List<SearchResult>> {
                    launch(Dispatchers.IO) {
                        val r = try { searchService.fetchHomeFeed() } catch (e: Exception) { emptyList() }
                        if (r.isNotEmpty()) send(r)
                    }
                    launch(Dispatchers.IO) {
                        val r = try {
                            searchService.searchMusic(trendingQuery, songsOnly = true, maxPages = 1)
                                .ifEmpty { searchService.searchMusic(trendingQuery, songsOnly = false, maxPages = 1) }
                        } catch (e: Exception) { emptyList() }
                        if (r.isNotEmpty()) send(r)
                    }
                }.firstOrNull() ?: emptyList()

                val distinct = results.distinctBy { it.id }
                _trendingSongs.value = distinct
                trendingPage = 1
                // Load quick picks separately with a different query so they differ from trending
                if (_quickPicks.value.isEmpty()) {
                    launch {
                        try {
                            val qpResults = searchService.searchMusic(getRandomQuickPickQuery(), songsOnly = true, maxPages = 1)
                                .ifEmpty { searchService.searchMusic(getRandomQuickPickQuery(), songsOnly = false, maxPages = 1) }
                            val trendingIds = distinct.map { it.id }.toHashSet()
                            val uniquePicks = qpResults.distinctBy { it.id }.filter { it.id !in trendingIds }
                            _quickPicks.value = uniquePicks.take(6).ifEmpty { qpResults.distinctBy { it.id }.take(6) }
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Failed to load quick picks", e)
                        }
                    }
                }
                // Prefetch audio URLs for the first 20 results so inline play is instant
                val toWarm = distinct.filter { it.isPlayable }.take(20)
                launch(Dispatchers.IO) {
                    toWarm.forEach { youTubeStreamService.prefetchAudioUrl(it.id) }
                }
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
                    val songOnlyResults = searchService.searchMusic(query, songsOnly = true, maxPages = 2)
                    val results = if (songOnlyResults.isNotEmpty()) {
                        songOnlyResults
                    } else {
                        searchService.searchMusic(query, songsOnly = false, maxPages = 2)
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

    fun reloadTrending() {
        trendingPage = 0
        _trendingSongs.value = emptyList()
        _quickPicks.value = emptyList()
        loadTrendingSongs()
    }

    fun playSearchResult(searchResult: SearchResult) {
        if (!searchResult.isPlayable) return

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

    /** Play or pause a preview of [searchResult] inline without navigating away. */
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