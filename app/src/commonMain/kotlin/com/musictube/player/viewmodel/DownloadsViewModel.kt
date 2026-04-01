package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import com.musictube.player.platform.DownloadController
import com.musictube.player.service.DownloadStatus
import kotlinx.coroutines.flow.StateFlow

class DownloadsViewModel(
    private val downloadController: DownloadController
) : ViewModel() {

    val downloadStatus   : StateFlow<Map<String, DownloadStatus>> = downloadController.downloadStatus
    val downloadProgress : StateFlow<Map<String, Int>>            = downloadController.downloadProgress
    val downloadErrors   : StateFlow<Map<String, String>>         = downloadController.downloadErrors
    val downloadQueue    = downloadController.downloadQueue

    fun retryDownload(videoId: String) {
        downloadController.resetDownloadStatus(videoId)
        val result = downloadQueue.value[videoId] ?: return
        downloadController.downloadSong(result)
    }
}
