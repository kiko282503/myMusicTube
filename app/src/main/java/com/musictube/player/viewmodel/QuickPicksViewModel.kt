package com.musictube.player.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.service.SearchService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickPicksViewModel @Inject constructor(
    private val searchService: SearchService,
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    private val allQuickPickQueries = listOf(
        "trending music 2026", "top hits 2026", "viral songs 2026",
        "new releases 2026", "popular music 2026", "chart topper 2026",
        "best songs 2026", "hit music 2026", "latest songs 2026",
        "music trends 2026", "hot tracks 2026", "current favorites 2026",
        "popular tracks 2026", "trending now 2026", "viral music 2026",
        "top charts 2026", "music hits 2026", "trending songs 2026"
    )
    private var page = 0
    
    private fun getRandomQuery(): String {
        return allQuickPickQueries.random()
    }

    private val _songs = MutableStateFlow<List<SearchResult>>(emptyList())
    val songs: StateFlow<List<SearchResult>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    init {
        loadMore()
    }

    fun loadMore() {
        if (_isLoading.value || _isLoadingMore.value) return
        viewModelScope.launch {
            if (page == 0) _isLoading.value = true else _isLoadingMore.value = true
            try {
                val query = getRandomQuery()
                page++
                val results = searchService.searchMusic(query, songsOnly = true)
                _songs.value = _songs.value + results
            } catch (e: Exception) {
                Log.e("QuickPicksViewModel", "Failed to load picks", e)
            } finally {
                _isLoading.value = false
                _isLoadingMore.value = false
            }
        }
    }

    fun playSearchResult(searchResult: SearchResult) {
        playerManager.playYouTubeAudioStream(
            videoId = searchResult.id,
            title = searchResult.title,
            artist = searchResult.artist,
            thumbnailUrl = searchResult.thumbnailUrl
        )
    }
}
