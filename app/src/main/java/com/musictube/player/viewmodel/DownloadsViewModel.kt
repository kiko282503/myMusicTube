package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.DownloadManager
import com.musictube.player.service.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager
) : ViewModel() {

    val downloadQueue: StateFlow<Map<String, SearchResult>> = downloadManager.downloadQueue
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = downloadManager.downloadStatus
    val downloadProgress: StateFlow<Map<String, Int>> = downloadManager.downloadProgress
    val downloadErrors: StateFlow<Map<String, String>> = downloadManager.downloadErrors

    /** Reset a failed download and re-enqueue it. */
    fun retryDownload(searchResult: SearchResult) {
        downloadManager.resetDownloadStatus(searchResult.id)
        downloadManager.downloadSong(searchResult)
    }
}
