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
import com.musictube.player.service.YouTubeStreamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val musicRepository: MusicRepository,
    private val playerController: AudioPlayerController,
    private val searchService: SearchService,
    private val downloadController: DownloadController,
    private val youTubeStreamService: YouTubeStreamService
) : ViewModel() {

    private val offlinePlaylistName = "Offline Downloads"

    private val allTrendingQueries = listOf(
        "trending music 2026", "viral hits 2026", "chart toppers 2026",
        "new music releases 2026", "popular songs 2026", "top hits 2026",
        "best songs 2026", "hit music 2026", "latest songs 2026",
        "music trends 2026", "hot songs 2026", "trending now 2026"
    )
    private val allQuickPickQueries = listOf(
        "classic rock hits", "feel good songs", "chill vibes playlist",
        "throwback hits", "acoustic songs", "indie music favorites",
        "road trip songs", "workout music", "relaxing music",
        "80s greatest hits", "90s pop hits", "soft rock classics"
    )
    private var trendingPage = 0

    private val _isLoading      = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore  = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _trendingSongs  = MutableStateFlow<List<SearchResult>>(emptyList())
    val trendingSongs: StateFlow<List<SearchResult>> = _trendingSongs.asStateFlow()

    private val _quickPicks     = MutableStateFlow<List<SearchResult>>(emptyList())
    val quickPicks: StateFlow<List<SearchResult>> = _quickPicks.asStateFlow()

    val downloadStatus   : StateFlow<Map<String, DownloadStatus>> = downloadController.downloadStatus
    val downloadProgress : StateFlow<Map<String, Int>>            = downloadController.downloadProgress

    val downloadedVideoIds: StateFlow<Set<String>> = musicRepository.getDownloadedSongs()
        .map { songs -> songs.mapNotNull { if (it.id.startsWith("yt_")) it.id.removePrefix("yt_") else null }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val playlists: StateFlow<List<Playlist>> = musicRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val offlinePlaylist: StateFlow<Playlist?> = playlists
        .map { list -> list.firstOrNull { it.name == offlinePlaylistName } ?: list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val offlinePlaylistId  : StateFlow<String?> = offlinePlaylist.map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val offlineSongCount   : StateFlow<Int> = offlinePlaylist.map { it?.songCount ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val songs: StateFlow<List<Song>> = musicRepository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val currentSong     = playerController.currentSong
    val isPlaying       = playerController.isPlaying
    val isPlayingNow    = playerController.isPlaying
    val isFeedLoading   = _isLoading
    val currentPosition = playerController.currentPosition
    val duration        = playerController.duration
    val playQueueSize   = playerController.playQueueSize
    val previewVideoId  = playerController.currentVideoId
    val previewIsPlaying = playerController.isPlaying
    val previewIsLoading = playerController.isLoadingStream

    val recentSongs: StateFlow<List<Song>> = songs

    val likedSongs: StateFlow<List<Song>> = musicRepository.getLikedSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val homeFeeds: StateFlow<Map<String, List<SearchResult>>> =
        combine(_trendingSongs, _quickPicks) { trending, picks ->
            buildMap {
                if (trending.isNotEmpty()) put("Trending Now", trending.take(20))
                if (picks.isNotEmpty()) put("Quick Picks", picks)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    init { loadTrendingSongs() }

    fun loadTrendingSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val feedResults = searchService.fetchHomeFeed()
                if (feedResults.size >= 10) {
                    _trendingSongs.value = feedResults.take(50)
                } else {
                    val fallback = searchService.searchMusic(allTrendingQueries.random(), maxPages = 2)
                    _trendingSongs.value = (feedResults + fallback).distinctBy { it.id }.take(50)
                }
                val quickQuery = allQuickPickQueries.random()
                val picks = searchService.searchMusic(quickQuery, songsOnly = true, maxPages = 1)
                _quickPicks.value = picks.take(20)
            } catch (e: Exception) {
                val fallback = searchService.searchMusic(allTrendingQueries.random(), maxPages = 2)
                _trendingSongs.value = fallback
            } finally { _isLoading.value = false }
        }
    }

    fun loadMoreTrending() {
        if (_isLoadingMore.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val queries = allTrendingQueries.shuffled().take(3)
                val results = mutableListOf<SearchResult>()
                queries.forEach { q ->
                    launch(Dispatchers.Default) {
                        val r = searchService.searchMusic(q, maxPages = 1)
                        synchronized(results) { results.addAll(r) }
                    }
                }
                val merged = (_trendingSongs.value + results).distinctBy { it.id }
                _trendingSongs.value = merged
            } finally { _isLoadingMore.value = false }
        }
    }

    fun togglePreview(searchResult: SearchResult) {
        val currentId = playerController.currentVideoId.value
        when {
            currentId == searchResult.id && playerController.isPlaying.value -> playerController.pause()
            currentId == searchResult.id                                      -> playerController.resume()
            else -> {
                val song = Song(id = searchResult.id, title = searchResult.title,
                    artist = searchResult.artist, thumbnailUrl = searchResult.thumbnailUrl, isLocal = false)
                playerController.playYouTubeAudioStream(searchResult.id, song)
            }
        }
    }

    fun downloadSong(searchResult: SearchResult) {
        viewModelScope.launch {
            musicRepository.createPlaylist(offlinePlaylistName)
            downloadController.downloadSong(searchResult, offlinePlaylistName)
        }
    }

    fun pause()    = playerController.pause()
    fun resume()   = playerController.resume()
    fun playNext() = playerController.playNext()

    fun playSong(song: Song) {
        val videoId = song.id.removePrefix("yt_")
        playerController.playYouTubeAudioStream(videoId, song)
    }

    fun toggleLike(song: Song) {
        viewModelScope.launch {
            musicRepository.updateLikedStatus(song.id, !song.isLiked)
        }
    }

    fun renamePlaylist(id: String, newName: String) =
        viewModelScope.launch { musicRepository.renamePlaylist(id, newName.trim()) }

    fun deletePlaylist(id: String) =
        viewModelScope.launch { musicRepository.deletePlaylist(id) }

    fun createPlaylist(name: String) =
        viewModelScope.launch { musicRepository.createPlaylist(name.trim()) }
}
