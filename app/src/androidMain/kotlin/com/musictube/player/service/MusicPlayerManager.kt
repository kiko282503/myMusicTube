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
import com.musictube.player.platform.AudioPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MusicPlayerManager(
    private val context: Context,
    private val audioExtractor: YouTubeAudioExtractor,
    private val localAudioManager: LocalAudioManager,
    private val youTubeStreamService: com.musictube.player.service.YouTubeStreamService
) : AudioPlayerController {

    private val _currentSong = MutableStateFlow<Song?>(null)
    override val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _currentVideoId = MutableStateFlow<String?>(null)
    override val currentVideoId: StateFlow<String?> = _currentVideoId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isLoadingStream = MutableStateFlow(false)
    override val isLoadingStream: StateFlow<Boolean> = _isLoadingStream.asStateFlow()

    private val _isShuffleOn = MutableStateFlow(false)
    override val isShuffleOn: StateFlow<Boolean> = _isShuffleOn.asStateFlow()

    private val _isRepeatOn = MutableStateFlow(false)
    override val isRepeatOn: StateFlow<Boolean> = _isRepeatOn.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Playlist queue
    private val _playQueue = MutableStateFlow<List<Song>>(emptyList())
    private val _queueIndex = MutableStateFlow(-1)
    private var _isPlayingViaQueue = false
    private val _shuffleHistory = mutableListOf<Int>()

    override val playQueueSize: StateFlow<Int> = _playQueue
        .map { it.size }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    override val hasPrevious: StateFlow<Boolean> = combine(_playQueue, _queueIndex) { queue, idx ->
        queue.isNotEmpty() && idx > 0
    }.stateIn(scope, SharingStarted.Eagerly, false)

    override val hasNext: StateFlow<Boolean> = combine(_playQueue, _queueIndex) { queue, idx ->
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
                            if (_playQueue.value.isNotEmpty()) advanceQueue()
                            else { exoPlayer.seekTo(0); exoPlayer.pause() }
                        }
                        else -> {}
                    }
                }
            })
        }
    }

    init {
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

    override fun playSong(song: Song) {
        if (!_isPlayingViaQueue) {
            _playQueue.value = emptyList()
            _queueIndex.value = -1
        }
        val previousSongId = _currentSong.value?.id
        val currentDuration = _duration.value
        _currentSong.value = song
        _currentVideoId.value = null
        _currentPosition.value = 0L
        if (previousSongId != song.id || currentDuration == 0L) _duration.value = 0L

        val mediaItem = MediaItem.Builder()
            .setUri(song.filePath ?: song.url)
            .setMediaId(song.id)
            .build()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        startNotificationService(song.title, song.artist, song.thumbnailUrl)
    }

    override fun setPlaylistQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        _playQueue.value = songs
        _queueIndex.value = clampedIndex
        _shuffleHistory.clear()
        _shuffleHistory.add(clampedIndex)
        playQueueItemAt(clampedIndex)
    }

    private fun advanceQueue() {
        val queue = _playQueue.value
        val qSize = queue.size
        if (qSize == 0) return
        val currentIdx = _queueIndex.value

        if (_isShuffleOn.value) {
            if (currentIdx >= 0 && currentIdx !in _shuffleHistory) _shuffleHistory.add(currentIdx)
            if (_shuffleHistory.size >= qSize) {
                if (_isRepeatOn.value) {
                    _shuffleHistory.clear()
                    if (currentIdx >= 0) _shuffleHistory.add(currentIdx)
                } else return
            }
            val available = (0 until qSize).filter { it !in _shuffleHistory }
            if (available.isEmpty()) return
            val nextIdx = available.random()
            _shuffleHistory.add(nextIdx)
            _queueIndex.value = nextIdx
            playQueueItemAt(nextIdx)
        } else if (currentIdx < qSize - 1) {
            val nextIdx = currentIdx + 1
            _queueIndex.value = nextIdx
            playQueueItemAt(nextIdx)
        } else if (_isRepeatOn.value) {
            _queueIndex.value = 0
            playQueueItemAt(0)
        }
    }

    override fun playNext() {
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

    override fun playPrevious() {
        val idx = _queueIndex.value
        if (idx > 0) { _queueIndex.value = idx - 1; playQueueItemAt(idx - 1) }
    }

    private fun playQueueItemAt(index: Int) {
        val song = _playQueue.value.getOrNull(index) ?: return
        _isPlayingViaQueue = true
        if (song.isDownloaded && !song.filePath.isNullOrBlank()) {
            playSong(song)
        } else {
            val videoId = song.id.removePrefix("yt_").removePrefix("dl_")
            playYouTubeAudioStream(videoId, song)
        }
        _isPlayingViaQueue = false
    }

    // ── WebView (YouTube Music web fallback) ──────────────────────────────────
    private var _lastVideoId: String? = null
    private var _lastTitle: String = ""
    private var _lastArtist: String = ""
    private var _lastThumb: String? = null
    private var _webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(activityContext: Context): WebView {
        val wv = _webView ?: WebView(activityContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.allowContentAccess = true
            settings.allowFileAccess = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.safeBrowsingEnabled = false
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.119 Mobile Safari/537.36"
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

        val activity = activityContext as? Activity
        val contentView = activity?.findViewById<android.view.ViewGroup>(android.R.id.content)
        if (contentView != null && wv.parent == null) {
            wv.visibility = android.view.View.VISIBLE
            contentView.addView(wv, android.view.ViewGroup.LayoutParams(1, 1))
            injectVideoMonitor()
        }
        return wv
    }

    private fun injectVideoMonitor() {
        val js = """
        (function(){
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
          function tryPlay(v){ if(!v||!v.paused||v.ended) return; v.play().catch(function(){
            var btn = document.querySelector('ytmusic-player-bar .play-pause-button[aria-label="Play"],[aria-label="Play"],.ytmusic-player-bar [title="Play"]');
            if(btn) btn.click(); }); }
          function attach() {
            var v = document.querySelector('video');
            if (!v) { setTimeout(attach, 500); return; }
            tryPlay(v);
            v.addEventListener('playing', function(){ Android.onPlaying(v.duration*1000); });
            v.addEventListener('pause',   function(){ if(!v.ended) Android.onPaused(); });
            v.addEventListener('ended',   function(){ Android.onEnded(); });
            v.addEventListener('error',   function(){ Android.onVideoError(); });
            v.addEventListener('durationchange', function(){
              if(v.duration&&isFinite(v.duration)) Android.onDuration(v.duration*1000); });
            setInterval(function(){
              if(!v.paused && !v.ended && v.readyState>=2)
                Android.onPosition(v.currentTime*1000, v.duration*1000);
              else if(v.paused && !v.ended && v.readyState>=3 && !window._ytIntentionalPause)
                tryPlay(v);
            }, 1000);
          }
          attach();
        })();
        """.trimIndent()
        _webView?.post { _webView?.evaluateJavascript(js, null) }
    }

    override fun replayCurrent() {
        val vid = _lastVideoId
        if (vid != null) {
            if (_currentVideoId.value == vid && exoPlayer.playbackState != Player.STATE_IDLE) {
                exoPlayer.seekTo(0); exoPlayer.play()
                startNotificationService(_lastTitle, _lastArtist, _lastThumb)
            } else {
                playYouTubeAudioStream(vid, Song(id = "yt_$vid", title = _lastTitle, artist = _lastArtist, thumbnailUrl = _lastThumb, isLocal = false))
            }
        } else {
            _currentSong.value?.let { playSong(it) }
        }
    }

    fun resumeWebView() {
        _webView?.let { wv ->
            wv.resumeTimers(); wv.onResume(); injectVideoMonitor()
            wv.post { wv.evaluateJavascript("var v=document.querySelector('video'); if(v&&v.paused&&!v.ended) v.play();", null) }
        }
    }

    fun parkWebView(activityContext: Context) {
        val wv = _webView ?: return
        wv.resumeTimers(); wv.onResume()
        wv.post {
            wv.evaluateJavascript("""
                (function(){
                  try {
                    Object.defineProperty(document,'hidden',{get:function(){return false;}});
                    Object.defineProperty(document,'visibilityState',{get:function(){return 'visible';}});
                  } catch(e){}
                  if(!window._visHooked){
                    window._visHooked=true;
                    document.addEventListener('visibilitychange',function(e){ e.stopImmediatePropagation(); e.preventDefault(); },true);
                    setInterval(function(){ var v=document.querySelector('video'); if(v&&v.paused&&!v.ended&&!window._ytIntentionalPause) v.play().catch(function(){}); },1000);
                  }
                })();
            """.trimIndent(), null)
        }
    }

    fun startNotificationService(title: String, artist: String, thumbnailUrl: String?) {
        val intent = Intent(context, MusicPlayerService::class.java).apply {
            putExtra("title", title)
            putExtra("artist", artist)
            putExtra("thumbnail", thumbnailUrl)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
        else context.startService(intent)
    }

    override fun playYouTubeAudioStream(videoId: String, songInfo: Song?) {
        val title = songInfo?.title ?: ""
        val artist = songInfo?.artist ?: ""
        val thumbnailUrl = songInfo?.thumbnailUrl
        if (!_isPlayingViaQueue) { _playQueue.value = emptyList(); _queueIndex.value = -1 }
        _lastVideoId = videoId; _lastTitle = title; _lastArtist = artist; _lastThumb = thumbnailUrl
        exoPlayer.stop()
        _currentSong.value = Song(id = "yt_$videoId", title = title, artist = artist,
            thumbnailUrl = thumbnailUrl, isLocal = false, url = "")
        _currentVideoId.value = videoId
        _isPlaying.value = false; _currentPosition.value = 0L; _duration.value = 0L
        startNotificationService(title, artist, thumbnailUrl)
        _isLoadingStream.value = true
        scope.launch {
            val audioUrl = youTubeStreamService.extractAudioUrl(videoId)
            if (audioUrl != null) {
                val mediaItem = MediaItem.Builder().setUri(audioUrl).setMediaId("yt_$videoId").build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
            } else {
                _isLoadingStream.value = false
                android.util.Log.e("MusicPlayerManager", "Failed to extract audio URL for $videoId")
            }
        }
    }

    inner class YouTubeInterface {
        @android.webkit.JavascriptInterface
        fun onPlaying(durationMs: Double) { scope.launch { _isPlaying.value = true; if (durationMs > 0 && durationMs.isFinite()) _duration.value = durationMs.toLong() } }
        @android.webkit.JavascriptInterface
        fun onPaused() { scope.launch { _isPlaying.value = false } }
        @android.webkit.JavascriptInterface
        fun onEnded() { scope.launch { _isPlaying.value = false; _currentPosition.value = 0L; if (_playQueue.value.isNotEmpty()) advanceQueue() } }
        @android.webkit.JavascriptInterface
        fun onDuration(durationMs: Double) { scope.launch { if (durationMs > 0 && durationMs.isFinite()) _duration.value = durationMs.toLong() } }
        @android.webkit.JavascriptInterface
        fun onPosition(positionMs: Double, durationMs: Double) { scope.launch { _currentPosition.value = positionMs.toLong(); if (durationMs > 0 && durationMs.isFinite()) _duration.value = durationMs.toLong() } }
        @android.webkit.JavascriptInterface
        fun onVideoError() { scope.launch { android.util.Log.e("MusicPlayerManager", "YouTube video element error"); _isPlaying.value = false } }
    }

    override fun pause() { exoPlayer.pause() }

    override fun resume() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
        exoPlayer.play()
    }

    override fun stop() {
        try {
            exoPlayer.stop(); _isPlaying.value = false; _currentPosition.value = 0L
            val intent = Intent(context, MusicPlayerService::class.java).apply { action = MusicPlayerService.ACTION_STOP }
            context.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerManager", "Error stopping playback", e)
        }
    }

    override fun seekTo(position: Long) { exoPlayer.seekTo(position); _currentPosition.value = position }

    override fun setVolume(volume: Float) { _volume.value = volume.coerceIn(0f, 1f); exoPlayer.volume = _volume.value }

    override fun toggleShuffle() { _isShuffleOn.value = !_isShuffleOn.value }
    override fun toggleRepeat() { _isRepeatOn.value = !_isRepeatOn.value }

    fun release() { scope.cancel(); exoPlayer.release() }
    fun getSampleSongs() = localAudioManager.getSampleAudioFiles()
}
