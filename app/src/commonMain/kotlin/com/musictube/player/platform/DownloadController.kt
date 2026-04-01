package com.musictube.player.platform

import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.DownloadStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic abstraction over file download management.
 *
 * Android: backed by OkHttpDownloader + DownloadForegroundService.
 * iOS:     backed by URLSession background downloads.
 */
interface DownloadController {
    val downloadStatus: StateFlow<Map<String, DownloadStatus>>
    val downloadProgress: StateFlow<Map<String, Int>>
    val downloadErrors: StateFlow<Map<String, String>>
    val downloadQueue: StateFlow<Map<String, SearchResult>>

    fun downloadSong(searchResult: SearchResult, playlistName: String = "Offline Downloads")
    fun resetDownloadStatus(videoId: String)
}
