package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.platform.AudioPlayerController
import com.musictube.player.platform.DownloadController
import com.musictube.player.service.DownloadStatus
import com.musictube.player.service.SearchService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuickPicksViewModel(
    private val searchService: SearchService,
    private val playerController: AudioPlayerController,
    private val downloadController: DownloadController
) : ViewModel() {

    private val queries = listOf(
        "classic rock hits", "feel good songs", "chill vibes",
        "throwback hits", "acoustic songs", "indie music favorites",
        "road trip songs", "workout music", "relaxing music"
    )

    private val _picks     = MutableStateFlow<List<SearchResult>>(emptyList())
    val picks: StateFlow<List<SearchResult>> = _picks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val downloadStatus   : StateFlow<Map<String, DownloadStatus>> = downloadController.downloadStatus
    val downloadProgress : StateFlow<Map<String, Int>>            = downloadController.downloadProgress
    val previewVideoId   = playerController.currentVideoId
    val previewIsPlaying = playerController.isPlaying
    val previewIsLoading = playerController.isLoadingStream

    init { loadMore() }

    fun loadMore() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = searchService.searchMusic(queries.random(), songsOnly = true, maxPages = 1)
                _picks.value = (_picks.value + results).distinctBy { it.id }
            } finally { _isLoading.value = false }
        }
    }

    fun playSearchResult(result: SearchResult) {
        val song = Song(id = result.id, title = result.title, artist = result.artist,
            thumbnailUrl = result.thumbnailUrl, isLocal = false)
        playerController.playYouTubeAudioStream(result.id, song)
    }

    fun togglePreview(result: SearchResult) {
        val currentId = playerController.currentVideoId.value
        when {
            currentId == result.id && playerController.isPlaying.value -> playerController.pause()
            currentId == result.id                                      -> playerController.resume()
            else -> playSearchResult(result)
        }
    }
}
