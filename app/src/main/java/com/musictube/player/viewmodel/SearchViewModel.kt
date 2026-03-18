package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.service.SearchService
import com.musictube.player.service.YouTubeStreamService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

enum class DownloadStatus {
    IDLE, DOWNLOADING, COMPLETED, FAILED
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchService: SearchService,
    private val musicRepository: MusicRepository,
    private val playerManager: MusicPlayerManager,
    private val youTubeStreamService: YouTubeStreamService
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            searchMusic()
        } else {
            _searchResults.value = emptyList()
        }
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }
    
    fun searchMusic() {
        viewModelScope.launch {
            if (_searchQuery.value.isBlank()) return@launch
            
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val results = searchService.searchMusic(_searchQuery.value)
                _searchResults.value = results
                
                if (results.isEmpty()) {
                    _errorMessage.value = "No results found for '${_searchQuery.value}'"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun playSearchResult(searchResult: SearchResult) {
        // Use YouTube WebView embed - no audio extraction needed
        playerManager.playYouTubeAudioStream(
            videoId = searchResult.id,
            title = searchResult.title,
            artist = searchResult.artist,
            thumbnailUrl = searchResult.thumbnailUrl
        )
    }
    
    fun downloadSong(searchResult: SearchResult) {
        viewModelScope.launch {
            // Update download status to downloading
            val currentStatus = _downloadStatus.value.toMutableMap()
            currentStatus[searchResult.id] = DownloadStatus.DOWNLOADING
            _downloadStatus.value = currentStatus
            
            try {
                // Simulate download progress (in real app, this would be actual download)
                kotlinx.coroutines.delay(2000)
                
                // Convert search result to Song and save to database
                // Use the extracted audio URL or fall back to demo
                val audioUrl = searchResult.audioUrl ?: generateDemoAudioUrl(searchResult.title)
                
                val song = Song(
                    id = UUID.randomUUID().toString(),
                    title = searchResult.title,
                    artist = searchResult.artist,
                    duration = parseDuration(searchResult.duration),
                    url = audioUrl,
                    thumbnailUrl = searchResult.thumbnailUrl,
                    isLocal = false,
                    isDownloaded = true
                )
                
                musicRepository.insertSong(song)
                
                // Update status to completed
                val updatedStatus = _downloadStatus.value.toMutableMap()
                updatedStatus[searchResult.id] = DownloadStatus.COMPLETED
                _downloadStatus.value = updatedStatus
                
                // TODO: Implement actual file download logic here
                // Real implementation would:
                // 1. Use NewPipeExtractor or similar to get audio stream URL
                // 2. Download audio file to local storage
                // 3. Update song with local file path
                
            } catch (e: Exception) {
                // Update status to failed
                val failedStatus = _downloadStatus.value.toMutableMap()
                failedStatus[searchResult.id] = DownloadStatus.FAILED
                _downloadStatus.value = failedStatus
                
                _errorMessage.value = "Download failed: ${e.message}"
            }
        }
    }
    
    private fun generateDemoAudioUrl(title: String): String {
        // Generate demo audio URLs that actually work for testing
        // These are short audio samples for demonstration
        val demoUrls = listOf(
            "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav",
            "https://www.soundjay.com/misc/sounds/fail-buzzer-02.wav", 
            "https://www.soundjay.com/misc/sounds/beep-07a.wav",
            "https://www.soundjay.com/misc/sounds/beep-10.wav",
            "https://www.soundjay.com/misc/sounds/beep-22.wav"
        )
        // Return a demo URL based on title hash for consistency
        return demoUrls[kotlin.math.abs(title.hashCode()) % demoUrls.size]
    }

    private fun parseDuration(durationStr: String): Long {
        // Parse duration string (e.g., "3:45") to milliseconds
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