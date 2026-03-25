package com.musictube.player.service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musictube.player.data.model.Song
import com.musictube.player.service.YouTubeAudioExtractor
import com.musictube.player.service.LocalAudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioExtractor: YouTubeAudioExtractor,
    private val localAudioManager: LocalAudioManager,
    private val youTubeStreamService: YouTubeStreamService
) {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Playlist queue
    private val _playQueue = MutableStateFlow<List<Song>>(emptyList())
    private val _queueIndex = MutableStateFlow(-1)
    private var _isPlayingViaQueue = false

    // Shuffle & repeat modes
    private val _isShuffleOn = MutableStateFlow(false)
    val isShuffleOn: StateFlow<Boolean> = _isShuffleOn.asStateFlow()

    private val _isRepeatOn = MutableStateFlow(false)
    val isRepeatOn: StateFlow<Boolean> = _isRepeatOn.asStateFlow()

    // True while extracting a stream URL from YouTube (before ExoPlayer starts)
    private val _isLoadingStream = MutableStateFlow(false)
    val isLoadingStream: StateFlow<Boolean> = _isLoadingStream.asStateFlow()

    // Tracks which song IDs have been played in the current shuffle cycle
    private val _shuffleHistory = mutableListOf<Int>()

    fun toggleShuffle() { _isShuffleOn.value = !_isShuffleOn.value }
    fun toggleRepeat()  { _isRepeatOn.value  = !_isRepeatOn.value  }

    /** Number of songs in the active queue. 0 when no queue is set. */
    val playQueueSize: StateFlow<Int> = _playQueue
        .map { it.size }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val hasPrevious: StateFlow<Boolean> = combine(_playQueue, _queueIndex) { queue, idx ->
        queue.isNotEmpty() && idx > 0
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val hasNext: StateFlow<Boolean> = combine(_playQueue, _queueIndex) { queue, idx ->
        queue.isNotEmpty() && (idx < queue.size - 1 || _isRepeatOn.value || _isShuffleOn.value)
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) _isLoadingStream.value = false
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            val dur = exoPlayer.duration
                            if (dur > 0) _duration.value = dur
                        }
                        Player.STATE_ENDED -> {
                            _isPlaying.value = false
                            _currentPosition.value = 0L
                            val qIdx = _queueIndex.value
                            val qSize = _playQueue.value.size
                            if (qSize > 0) {
                                advanceQueue()
                            } else {
                                exoPlayer.seekTo(0)
                                exoPlayer.pause()
                            }
                        }
                        else -> {}
                    }
                }
            })
        }
    }

    init {
        // Poll playback position every 500ms so the UI timestamp updates live
        scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    _currentPosition.value = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
                    if (dur > 0) _duration.value = dur
                }
                delay(500)
            }
        }
    }

    fun playSong(song: Song) {
        if (!_isPlayingViaQueue) {
            _playQueue.value = emptyList()
            _queueIndex.value = -1
        }
        val previousSongId = _currentSong.value?.id
        val currentDuration = _duration.value
        
        _currentSong.value = song
        _currentVideoId.value = null  // clear any WebView video
        _currentPosition.value = 0L
        
        // Only reset duration to 0 if it's a different song
        // This prevents the "--:--" flicker when restarting the same song
        if (previousSongId != song.id || currentDuration == 0L) {
            _duration.value = 0L
        }

        val mediaItem = MediaItem.Builder()
            .setUri(song.filePath ?: song.url)
            .setMediaId(song.id)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        
        // Always start notification service when playing a song
        startNotificationService(song.title, song.artist, song.thumbnailUrl)
    }

    fun setPlaylistQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        _playQueue.value = songs
        _queueIndex.value = clampedIndex
        _shuffleHistory.clear()
        _shuffleHistory.add(clampedIndex)
        playQueueItemAt(clampedIndex)
    }

    /** Advance to the next song respecting shuffle and repeat modes. */
    private fun advanceQueue() {
        val queue = _playQueue.value
        val qSize = queue.size
        if (qSize == 0) return
        val currentIdx = _queueIndex.value

        if (_isShuffleOn.value) {
            // Always ensure the currently-playing index is recorded so it cannot be
            // immediately re-picked.
            if (currentIdx >= 0 && currentIdx !in _shuffleHistory) {
                _shuffleHistory.add(currentIdx)
            }
            android.util.Log.d("ShuffleQueue", "History after finishing idx=$currentIdx: $_shuffleHistory (qSize=$qSize, repeat=${_isRepeatOn.value})")

            // All songs in the playlist have been played.
            if (_shuffleHistory.size >= qSize) {
                if (_isRepeatOn.value) {
                    // Repeat ON → start a new cycle, avoid repeating the last song immediately.
                    android.util.Log.d("ShuffleQueue", "All songs played — repeat ON, starting new cycle")
                    _shuffleHistory.clear()
                    if (currentIdx >= 0) _shuffleHistory.add(currentIdx)
                } else {
                    // Repeat OFF → stop after the last song.
                    android.util.Log.d("ShuffleQueue", "All songs played — repeat OFF, stopping")
                    return
                }
            }

            val available = (0 until qSize).filter { it !in _shuffleHistory }
            if (available.isEmpty()) return   // single-song edge-case guard
            val nextIdx = available.random()
            android.util.Log.d("ShuffleQueue", "Picking idx=$nextIdx (${queue[nextIdx].title}) from available=$available")
            _shuffleHistory.add(nextIdx)
            _queueIndex.value = nextIdx
            playQueueItemAt(nextIdx)
        } else if (currentIdx < qSize - 1) {
            // Normal sequential advance
            val nextIdx = currentIdx + 1
            android.util.Log.d("ShuffleQueue", "Sequential: idx $currentIdx → $nextIdx (${queue[nextIdx].title})")
            _queueIndex.value = nextIdx
            playQueueItemAt(nextIdx)
        } else if (_isRepeatOn.value) {
            // At the end and repeat is on — wrap back to the start
            android.util.Log.d("ShuffleQueue", "Sequential end — repeat ON, wrapping to 0")
            _queueIndex.value = 0
            playQueueItemAt(0)
        } else {
            android.util.Log.d("ShuffleQueue", "Sequential end — repeat OFF, stopping")
        }
    }

    fun playNext() {
        val idx = _queueIndex.value
        val queue = _playQueue.value
        if (_isShuffleOn.value && queue.isNotEmpty()) {
            if (idx >= 0 && idx !in _shuffleHistory) _shuffleHistory.add(idx)
            if (_shuffleHistory.size >= queue.size) {
                _shuffleHistory.clear()
                if (idx >= 0) _shuffleHistory.add(idx)
            }
            val available = (0 until queue.size).filter { it !in _shuffleHistory }
            if (available.isEmpty()) return
            val nextIdx = available.random()
            _shuffleHistory.add(nextIdx)
            _queueIndex.value = nextIdx
            playQueueItemAt(nextIdx)
        } else if (idx < queue.size - 1) {
            _queueIndex.value = idx + 1
            playQueueItemAt(idx + 1)
        } else if (_isRepeatOn.value && queue.isNotEmpty()) {
            _queueIndex.value = 0
            playQueueItemAt(0)
        }
    }

    fun playPrevious() {
        val idx = _queueIndex.value
        if (idx > 0) {
            _queueIndex.value = idx - 1
            playQueueItemAt(idx - 1)
        }
    }

    private fun playQueueItemAt(index: Int) {
        val song = _playQueue.value.getOrNull(index) ?: return
        _isPlayingViaQueue = true
        val localPath = song.filePath
        if (song.isDownloaded && !localPath.isNullOrBlank()) {
            playSong(song)
        } else {
            val videoId = song.id.removePrefix("yt_").removePrefix("dl_")
            playYouTubeAudioStream(videoId, song.title, song.artist, song.thumbnailUrl)
        }
        _isPlayingViaQueue = false
    }

    // Persistent WebView — survives navigation changes so audio keeps playing on back press
    // Remember the last YouTube song so stop() → play() can resume it.
    private var _lastVideoId: String? = null
    private var _lastTitle: String = ""
    private var _lastArtist: String = ""
    private var _lastThumb: String? = null

    private var _webView: WebView? = null

    /** Returns the singleton WebView. When called with an Activity context and the WebView
     *  is not yet in a window, attaches it to android.R.id.content (VISIBLE 1×1px) so
     *  autoplay policy is satisfied — without ever moving it between parents. */
    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(context: Context): WebView {
        // Create once
        val wv = _webView ?: WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowContentAccess = true
            settings.allowFileAccess = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = false
            }
            // Use a real Chrome desktop UA — YouTube Music serves a richer page
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 12; Pixel 6) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/122.0.6261.119 Mobile Safari/537.36"
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    injectVideoMonitor()
                }
            }
            addJavascriptInterface(YouTubeInterface(), "Android")
        }.also { _webView = it }

        // Attach to the Activity window every time we have an Activity context and the
        // WebView has no parent yet.  The first call from playYouTubeAudioStream uses
        // @ApplicationContext so the cast returns null and we skip.  The call from
        // PlayerScreen uses the real Activity context, which will attach it at that point.
        val activity = context as? Activity
        val contentView = activity?.findViewById<android.view.ViewGroup>(android.R.id.content)
        if (contentView != null && wv.parent == null) {
            wv.visibility = android.view.View.VISIBLE
            contentView.addView(wv, android.view.ViewGroup.LayoutParams(1, 1))
            // Page may have already loaded (URL was loaded before we had a window).
            // Re-inject the monitor so tryPlay() runs now that we have a window token.
            injectVideoMonitor()
        }

        return wv
    }

    /**
     * Injects a JS poller into the WebView that watches the <video> element on
     * music.youtube.com and bridges play/pause/ended/timeupdate events back to Android.
     */
    private fun injectVideoMonitor() {
        val js = """
        (function(){
          // Spoof visibility API so YouTube doesn't pause on background
          try {
            Object.defineProperty(document,'hidden',{get:function(){return false;}});
            Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';}});
          } catch(e){}
          if(!window._visHooked){
            window._visHooked=true;
            document.addEventListener('visibilitychange',function(e){
              e.stopImmediatePropagation();e.preventDefault();
            },true);
          }

          if(window._ytBridgeInstalled) return;
          window._ytBridgeInstalled = true;

          function tryPlay(v){
            if(!v||!v.paused||v.ended) return;
            v.play().catch(function(){
              // Fall back to clicking YouTube Music's own play button
              var btn = document.querySelector(
                'ytmusic-player-bar .play-pause-button[aria-label="Play"],'+
                '[aria-label="Play"],'+
                '.ytmusic-player-bar [title="Play"]'
              );
              if(btn) btn.click();
            });
          }

          function attach() {
            var v = document.querySelector('video');
            if (!v) { setTimeout(attach, 500); return; }
            tryPlay(v);
            v.addEventListener('playing', function(){ Android.onPlaying(v.duration*1000); });
            v.addEventListener('pause',   function(){ if(!v.ended) Android.onPaused(); });
            v.addEventListener('ended',   function(){ Android.onEnded(); });
            v.addEventListener('error',   function(){ Android.onVideoError(); });
            v.addEventListener('durationchange', function(){
              if(v.duration&&isFinite(v.duration)) Android.onDuration(v.duration*1000);
            });
            setInterval(function(){
              if(!v.paused && !v.ended && v.readyState>=2)
                Android.onPosition(v.currentTime*1000, v.duration*1000);
              else if(v.paused && !v.ended && v.readyState>=3 && !window._ytIntentionalPause)
                tryPlay(v); // retry only if pause was NOT intentional
            }, 1000);
          }
          attach();
        })();
        """.trimIndent()
        _webView?.post { _webView?.evaluateJavascript(js, null) }
    }

    /**
     * Replay the current song from the beginning.
     * For YouTube: if the page is already loaded for this videoId, seek to 0 and play via JS
     * (avoids a full page reload which triggers autoplay policy rejection).
     * For local tracks: restart via ExoPlayer.
     */
    fun replayCurrent() {
        val vid = _lastVideoId
        if (vid != null) {
            if (_currentVideoId.value == vid && exoPlayer.playbackState != Player.STATE_IDLE) {
                exoPlayer.seekTo(0)
                exoPlayer.play()
                startNotificationService(_lastTitle, _lastArtist, _lastThumb)
            } else {
                playYouTubeAudioStream(vid, _lastTitle, _lastArtist, _lastThumb)
            }
        } else {
            _currentSong.value?.let { playSong(it) }
        }
    }
    /** Resume the WebView timers when app returns to foreground. */
    fun resumeWebView() {
        _webView?.let { wv ->
            wv.resumeTimers()
            wv.onResume()
            injectVideoMonitor()
            wv.post { wv.evaluateJavascript(
                "var v=document.querySelector('video'); if(v&&v.paused&&!v.ended) v.play();", null) }
        }
    }

    /**
     * Called when the app goes to background. The WebView stays in android.R.id.content
     * (no view hierarchy change) — just keep timers running and spoof visibility.
     */
    fun parkWebView(context: Context) {
        val wv = _webView ?: return
        wv.resumeTimers()
        wv.onResume()
        wv.post {
            wv.evaluateJavascript("""
                (function(){
                  try {
                    Object.defineProperty(document,'hidden',{get:function(){return false;}});
                    Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';}});
                  } catch(e){}
                  if(!window._visHooked){
                    window._visHooked=true;
                    document.addEventListener('visibilitychange',function(e){
                      e.stopImmediatePropagation(); e.preventDefault();
                    },true);
                    setInterval(function(){
                      var v=document.querySelector('video');
                      if(v&&v.paused&&!v.ended&&!window._ytIntentionalPause) v.play().catch(function(){});
                    },1000);
                  }
                })();
            """.trimIndent(), null)
        }
    }

    // pauseWebViewPlayback / resumeWebViewPlayback removed — use pause() / resume() directly.

    /** Start / refresh the foreground notification service for the current song. */
    fun startNotificationService(title: String, artist: String, thumbnailUrl: String?) {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            putExtra("title", title)
            putExtra("artist", artist)
            putExtra("thumbnail", thumbnailUrl)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Play a YouTube song by extracting a direct audio stream URL and playing via ExoPlayer.
     * This bypasses the YouTube Music web player entirely, so no ads are served.
     * URL extraction uses a 5-strategy cascade: Innertube Android → NewPipe → Piped → Invidious → Cobalt.
     */
    fun playYouTubeAudioStream(videoId: String, title: String, artist: String, thumbnailUrl: String?) {
        if (!_isPlayingViaQueue) {
            _playQueue.value = emptyList()
            _queueIndex.value = -1
        }
        _lastVideoId = videoId
        _lastTitle = title
        _lastArtist = artist
        _lastThumb = thumbnailUrl

        exoPlayer.stop()

        _currentSong.value = Song(
            id = "yt_$videoId",
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            isLocal = false,
            url = ""
        )
        _currentVideoId.value = videoId
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L

        startNotificationService(title, artist, thumbnailUrl)

        _isLoadingStream.value = true
        scope.launch {
            val audioUrl = youTubeStreamService.extractAudioUrl(videoId)
            if (audioUrl != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(audioUrl)
                    .setMediaId("yt_$videoId")
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                // _isLoadingStream is cleared in onIsPlayingChanged(true)
            } else {
                _isLoadingStream.value = false
                android.util.Log.e("MusicPlayerManager", "Failed to extract audio URL for $videoId")
            }
        }
    }

    /** JavaScript interface bridging <video> element events into our StateFlows. */
    inner class YouTubeInterface {
        @android.webkit.JavascriptInterface
        fun onPlaying(durationMs: Double) {
            scope.launch {
                _isPlaying.value = true
                if (durationMs > 0 && durationMs.isFinite()) _duration.value = durationMs.toLong()
                android.util.Log.i("MusicPlayerManager", "YouTube playing, duration=${durationMs.toLong()}ms")
            }
        }

        @android.webkit.JavascriptInterface
        fun onPaused() {
            scope.launch { _isPlaying.value = false }
        }

        @android.webkit.JavascriptInterface
        fun onEnded() {
            scope.launch {
                _isPlaying.value = false
                _currentPosition.value = 0L
                if (_playQueue.value.isNotEmpty()) {
                    advanceQueue()
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun onDuration(durationMs: Double) {
            scope.launch {
                if (durationMs > 0 && durationMs.isFinite()) _duration.value = durationMs.toLong()
            }
        }

        @android.webkit.JavascriptInterface
        fun onPosition(positionMs: Double, durationMs: Double) {
            scope.launch {
                _currentPosition.value = positionMs.toLong()
                if (durationMs > 0 && durationMs.isFinite()) _duration.value = durationMs.toLong()
            }
        }

        @android.webkit.JavascriptInterface
        fun onVideoError() {
            scope.launch {
                android.util.Log.e("MusicPlayerManager", "YouTube video element error")
                _isPlaying.value = false
            }
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
        exoPlayer.play()
    }

    fun stop() {
        try {
            exoPlayer.stop()
            _isPlaying.value = false
            _currentPosition.value = 0L

            val intent = Intent(context, MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerManager", "Error stopping playback", e)
        }
    }

    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        _currentPosition.value = position
    }

    fun setVolume(volume: Float) {
        _volume.value = volume.coerceIn(0f, 1f)
        exoPlayer.volume = _volume.value
    }

    fun release() {
        scope.cancel()
        exoPlayer.release()
    }

    fun getCurrentPosition(): Long = exoPlayer.currentPosition

    fun getDuration(): Long = exoPlayer.duration

    /**
     * Get sample audio files that demonstrate working background playback
     */
    fun getSampleSongs(): List<Song> {
        return localAudioManager.getSampleAudioFiles()
    }
}