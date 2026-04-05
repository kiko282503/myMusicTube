package com.musictube.player.platform

import com.musictube.player.data.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic abstraction over the audio playback engine.
 *
 * Android: backed by ExoPlayer (MusicPlayerManager).
 * iOS:     backed by AVQueuePlayer (IosAudioPlayer).
 */
interface AudioPlayerController {
    val currentSong: StateFlow<Song?>
    val currentVideoId: StateFlow<String?>
    val isPlaying: StateFlow<Boolean>
    val isLoadingStream: StateFlow<Boolean>
    val lastError: StateFlow<String?>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val volume: StateFlow<Float>
    val hasPrevious: StateFlow<Boolean>
    val hasNext: StateFlow<Boolean>
    val isShuffleOn: StateFlow<Boolean>
    val isRepeatOn: StateFlow<Boolean>
    val playQueueSize: StateFlow<Int>

    fun playSong(song: Song)
    fun setPlaylistQueue(songs: List<Song>, startIndex: Int)
    fun playYouTubeAudioStream(videoId: String, songInfo: Song?)
    fun replayCurrent()
    fun pause()
    fun resume()
    fun stop()
    fun seekTo(position: Long)
    fun setVolume(volume: Float)
    fun playNext()
    fun playPrevious()
    fun toggleShuffle()
    fun toggleRepeat()
}
