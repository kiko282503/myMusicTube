package com.musictube.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.platform.DownloadController
import com.musictube.player.service.DownloadStatus
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val musicRepository: MusicRepository,
    private val downloadController: DownloadController
) : ViewModel() {

    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = downloadController.downloadStatus
}
