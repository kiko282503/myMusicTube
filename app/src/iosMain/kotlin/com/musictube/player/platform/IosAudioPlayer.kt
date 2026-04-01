package com.musictube.player.platform

import com.musictube.player.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * iOS stub implementation of AudioPlayerController.
 * TODO: Replace with AVQueuePlayer-backed implementation.
 */
class IosAudioPlayer : AudioPlayerController {
    override val currentSong: StateFlow<Song?> = MutableStateFlow(null)
    override val currentVideoId: StateFlow<String?> = MutableStateFlow(null)
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    override val isLoadingStream: StateFlow<Boolean> = MutableStateFlow(false)
    override val currentPosition: StateFlow<Long> = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = MutableStateFlow(0L)
    override val volume: StateFlow<Float> = MutableStateFlow(1f)
    override val hasPrevious: StateFlow<Boolean> = MutableStateFlow(false)
    override val hasNext: StateFlow<Boolean> = MutableStateFlow(false)
    override val isShuffleOn: StateFlow<Boolean> = MutableStateFlow(false)
    override val isRepeatOn: StateFlow<Boolean> = MutableStateFlow(false)
    override val playQueueSize: StateFlow<Int> = MutableStateFlow(0)

    override fun playSong(song: Song) {}
    override fun setPlaylistQueue(songs: List<Song>, startIndex: Int) {}
    override fun playYouTubeAudioStream(videoId: String, songInfo: Song?) {}
    override fun replayCurrent() {}
    override fun pause() {}
    override fun resume() {}
    override fun stop() {}
    override fun seekTo(position: Long) {}
    override fun setVolume(volume: Float) {}
    override fun playNext() {}
    override fun playPrevious() {}
    override fun toggleShuffle() {}
    override fun toggleRepeat() {}
}
