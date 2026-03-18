package com.musictube.player.viewmodel

import android.content.Context
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musictube.player.data.model.Song
import com.musictube.player.service.MusicPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: MusicPlayerManager
) : ViewModel() {

    val currentSong: StateFlow<Song?> = playerManager.currentSong
    val currentVideoId: StateFlow<String?> = playerManager.currentVideoId
    val isPlaying: StateFlow<Boolean> = playerManager.isPlaying
    val currentPosition: StateFlow<Long> = playerManager.currentPosition
    val duration: StateFlow<Long> = playerManager.duration
    val volume: StateFlow<Float> = playerManager.volume

    /** Returns the singleton WebView from MusicPlayerManager — creates it on first call. */
    fun getOrCreateWebView(context: Context): WebView = playerManager.getOrCreateWebView(context)

    /** Call when the app comes back to foreground so YouTube Music resumes if paused. */
    fun resumeWebView() = playerManager.resumeWebView()

    /** Park the WebView in the decor view so audio keeps playing when navigating back. */
    fun parkWebView(context: Context) = playerManager.parkWebView(context)

    fun play() { playerManager.replayCurrent() }
    fun pause() { playerManager.pause() }
    fun resume() { playerManager.resume() }
    fun stop() { playerManager.stop() }
    fun seekTo(position: Long) { playerManager.seekTo(position) }
    fun setVolume(volume: Float) { playerManager.setVolume(volume) }

    override fun onCleared() {
        super.onCleared()
        // Don't release the player — it should continue playing in background
    }
}