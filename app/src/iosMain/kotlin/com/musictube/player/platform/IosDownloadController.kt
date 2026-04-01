package com.musictube.player.platform

import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS stub implementation of DownloadController.
 * TODO: Replace with URLSession background download implementation.
 */
class IosDownloadController : DownloadController {
    override val downloadStatus: StateFlow<Map<String, DownloadStatus>> = MutableStateFlow(emptyMap())
    override val downloadProgress: StateFlow<Map<String, Int>> = MutableStateFlow(emptyMap())
    override val downloadErrors: StateFlow<Map<String, String>> = MutableStateFlow(emptyMap())
    override val downloadQueue: StateFlow<Map<String, SearchResult>> = MutableStateFlow(emptyMap())

    override fun downloadSong(searchResult: SearchResult, playlistName: String) {}
    override fun resetDownloadStatus(videoId: String) {}
}
