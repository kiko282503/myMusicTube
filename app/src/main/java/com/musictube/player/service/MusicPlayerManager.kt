package com.musictube.player.service

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.FrameLayout
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioExtractor: YouTubeAudioExtractor,
    private val localAudioManager: LocalAudioManager
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

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
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
                            exoPlayer.seekTo(0)
                            exoPlayer.pause()
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

    // Persistent WebView — survives navigation changes so audio keeps playing on back press
    private var _webView: WebView? = null

    /** Returns the singleton WebView, creating it with ApplicationContext if needed. */
    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(context: Context): WebView {
        return _webView ?: WebView(context.applicationContext).apply {
            settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 12; Pixel 5) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/120.0.6099.144 Mobile Safari/537.36"
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Enhanced settings for background playback
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            // Aggressive settings to prevent YouTube detection
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.blockNetworkLoads = false
            settings.loadsImagesAutomatically = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                settings.safeBrowsingEnabled = false
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val js = """
                        (function dismiss() {
                            var btns = document.querySelectorAll('button, [role=button], tp-yt-paper-button');
                            for (var b of btns) {
                                var t = (b.innerText || b.textContent || '').toLowerCase().trim();
                                if (t === 'accept all' || t === 'i agree' || t === 'agree' || t === 'accept') { b.click(); return; }
                                if (t === 'no thanks' || t === 'no, thanks' || t === 'skip') { b.click(); return; }
                            }
                            document.querySelectorAll('ytmusic-popup-container, tp-yt-iron-overlay-backdrop, [dialog-show], ytmusic-mealbar-promo-renderer')
                                .forEach(function(el) { el.style.display = 'none'; });
                        })();
                        if (!window._adInterval) {
                            window._adInterval = setInterval(function() {
                                var btns = document.querySelectorAll('button, [role=button], tp-yt-paper-button');
                                for (var b of btns) {
                                    var t = (b.innerText || b.textContent || '').toLowerCase().trim();
                                    if (t === 'no thanks' || t === 'no, thanks' || t === 'skip' || t === 'accept all' || t === 'i agree') b.click();
                                }
                                document.querySelectorAll('ytmusic-mealbar-promo-renderer, [dialog-show]')
                                    .forEach(function(el) { el.style.display = 'none'; });
                            }, 1000);
                        }
                    """.trimIndent()
                    view?.evaluateJavascript(js, null)
                }
            }
        }.also { _webView = it }
    }

    /** Call this when the app returns to foreground so YouTube Music resumes if it was paused. */
    fun resumeWebView() {
        _webView?.let { wv ->
            wv.resumeTimers()
            wv.onResume()
            // Also try to resume playback if it was paused
            wv.post {
                wv.evaluateJavascript(
                    """
                    // Check if video is paused and resume if needed
                    var video = document.querySelector('video');
                    if (video && video.paused) {
                        var playButton = document.querySelector('[data-title="Play"], [aria-label="Play"], [title="Play"], #play-pause-button, .play-pause-button, [role="button"][aria-label*="Play"]');
                        if (playButton) {
                            playButton.click();
                        }
                    }
                    """.trimIndent(),
                    null
                )
            }
        }
    }

    /**
     * Moves the WebView into the Activity's decor view so it stays attached to a window
     * while Compose navigates away from PlayerScreen — audio keeps playing uninterrupted.
     */
    fun parkWebView(context: Context) {
        val wv = _webView ?: return
        val decorView = (context as? Activity)?.window?.decorView as? android.view.ViewGroup ?: return
        // Detach from current Compose parent
        (wv.parent as? android.view.ViewGroup)?.removeView(wv)
        // Attach invisibly (1×1 px) so the WebView stays in-window and audio continues
        if (wv.parent == null) {
            decorView.addView(wv, FrameLayout.LayoutParams(1, 1))
        }
        // Keep WebView "active" by making it think it's still visible
        wv.resumeTimers()
        wv.onResume()
        
        // Aggressive approach: try to prevent YouTube from detecting background state
        wv.post {
            wv.evaluateJavascript(
                """
                // Override document visibility API to fake foreground state
                Object.defineProperty(document, "hidden", { value: false, writable: false });
                Object.defineProperty(document, "visibilityState", { value: "visible", writable: false });
                
                // Prevent YouTube from pausing on visibility change
                document.addEventListener('visibilitychange', function(e) {
                    e.stopImmediatePropagation();
                    e.preventDefault();
                }, true);
                
                // Intercept and resume any pauses
                setInterval(function() {
                    var video = document.querySelector('video');
                    if (video && video.paused && !video.ended) {
                        console.log('Detected pause, attempting resume...');
                        video.play().catch(function(e) {
                            console.log('Auto-play failed:', e);
                            // Fallback to button click
                            var playBtn = document.querySelector('[data-title="Play"], [aria-label*="Play"], .play-pause-button');
                            if (playBtn) playBtn.click();
                        });
                    }
                }, 1000);
                """.trimIndent(),
                null
            )
        }
    }

    /** Pause YouTube Music playback by injecting JS into the WebView. */
    fun pauseWebViewPlayback() {
        _isPlaying.value = false
        _webView?.post {
            _webView?.evaluateJavascript(
                """
                var playButton = document.querySelector('[data-title="Play"], [aria-label="Play"], [title="Play"], #play-pause-button, .play-pause-button, [role="button"][aria-label*="Play"]');
                if (playButton) {
                    playButton.click();
                } else {
                    // Fallback: try to find pause button and click it
                    var pauseButton = document.querySelector('[data-title="Pause"], [aria-label="Pause"], [title="Pause"], [role="button"][aria-label*="Pause"]');
                    if (pauseButton) pauseButton.click();
                }
                """.trimIndent(),
                null
            )
        }
    }

    /** Resume YouTube Music playback by injecting JS into the WebView. */
    fun resumeWebViewPlayback() {
        _isPlaying.value = true
        _webView?.post {
            _webView?.evaluateJavascript(
                """
                var playButton = document.querySelector('[data-title="Play"], [aria-label="Play"], [title="Play"], #play-pause-button, .play-pause-button, [role="button"][aria-label*="Play"]');
                if (playButton) {
                    playButton.click();
                } else {
                    // Alternative selectors for YouTube Music
                    var buttons = document.querySelectorAll('button, [role="button"]');
                    for (var btn of buttons) {
                        var ariaLabel = btn.getAttribute('aria-label') || '';
                        var title = btn.getAttribute('title') || btn.getAttribute('data-title') || '';
                        if (ariaLabel.includes('Play') || title.includes('Play')) {
                            btn.click();
                            break;
                        }
                    }
                }
                """.trimIndent(),
                null
            )
        }
    }

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

    /** Play using ExoPlayer only - always extract real YouTube audio, no fallbacks */
    fun playYouTubeAudioStream(videoId: String, title: String, artist: String, thumbnailUrl: String?) {
        scope.launch {
            android.util.Log.d("MusicPlayerManager", "Starting YouTube Music playback for: $title by $artist (ID: $videoId)")
            
            // Set loading state immediately
            _currentSong.value = Song(
                id = "yt_$videoId",
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                isLocal = false,
                url = ""
            )
            _currentVideoId.value = null
            _isPlaying.value = false // Not playing yet, still loading
            _currentPosition.value = 0L
            _duration.value = 0L
            
            // Show loading notification
            startNotificationService(title, artist, thumbnailUrl)
            
            try {
                android.util.Log.d("MusicPlayerManager", "Extracting YouTube audio stream...")
                
                // Always try to extract direct audio stream URL - no timeout, let it work
                val audioUrl = audioExtractor.extractAudioUrl(videoId)
                
                if (audioUrl != null) {
                    android.util.Log.i("MusicPlayerManager", "SUCCESS: Using real YouTube audio stream for $title")
                    
                    // Play the real YouTube audio
                    playDirectAudioStream(videoId, title, artist, thumbnailUrl, audioUrl)
                    
                } else {
                    android.util.Log.e("MusicPlayerManager", "FAILED: Could not extract YouTube audio for $title - trying alternative methods")
                    
                    // Try alternative extraction methods
                    retryYouTubeExtraction(videoId, title, artist, thumbnailUrl)
                }
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayerManager", "ERROR: Exception during YouTube extraction for $title", e)
                
                // Try alternative extraction instead of giving up
                retryYouTubeExtraction(videoId, title, artist, thumbnailUrl)
            }
        }
    }

    private fun playDirectAudioStream(videoId: String, title: String, artist: String, thumbnailUrl: String?, audioUrl: String) {
        exoPlayer.stop()
        _currentSong.value = Song(
            id = "stream_$videoId",
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            isLocal = false,
            url = audioUrl
        )
        _currentVideoId.value = null // Not using WebView
        _isPlaying.value = true
        _currentPosition.value = 0L
        _duration.value = 0L
        
        // Play direct audio stream
        val mediaItem = MediaItem.Builder()
            .setUri(audioUrl)
            .setMediaId("stream_$videoId")
            .build()
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        
        // Show notification
        startNotificationService(title, artist, thumbnailUrl)
    }
    
    /** Fallback to sample audio when YouTube extraction fails - starts immediately */
    private fun playSampleAudioFallback(videoId: String, title: String, artist: String, thumbnailUrl: String?) {
        android.util.Log.i("MusicPlayerManager", "Starting immediate sample audio playback for: $title")
        
        // Stop any current playback
        exoPlayer.stop()
        
        // Set song info immediately - no more "Loading..." state
        _currentSong.value = Song(
            id = "sample_$videoId",
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            isLocal = true,
            url = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-_bowls.mp3"
        )
        _currentVideoId.value = null // No WebView
        _isPlaying.value = true // Mark as playing immediately
        _currentPosition.value = 0L
        _duration.value = 0L
        
        // Play sample audio
        val sampleUrl = "https://commondatastorage.googleapis.com/codeskulptor-demos/DDR_assets/Sevish_-_bowls.mp3"
        val mediaItem = MediaItem.Builder()
            .setUri(android.net.Uri.parse(sampleUrl))
            .setMediaId("sample_$videoId")
            .build()
        
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        
        // Show notification
        startNotificationService(title, artist, thumbnailUrl)
    }
    
    /** Retry YouTube extraction with alternative methods */
    private suspend fun retryYouTubeExtraction(videoId: String, title: String, artist: String, thumbnailUrl: String?) {
        android.util.Log.d("MusicPlayerManager", "Retrying YouTube extraction with alternative methods for: $title")
        
        try {
            // Try YouTube Music specific URL format
            val musicUrl = "https://music.youtube.com/watch?v=$videoId"
            android.util.Log.d("MusicPlayerManager", "Trying YouTube Music URL: $musicUrl")
            
            // Try extracting from music.youtube.com instead of regular youtube.com
            val audioUrl = audioExtractor.extractFromYouTubeMusic(videoId)
            
            if (audioUrl != null) {
                android.util.Log.i("MusicPlayerManager", "SUCCESS: Extracted from YouTube Music for $title")
                playDirectAudioStream(videoId, title, artist, thumbnailUrl, audioUrl)
            } else {
                android.util.Log.e("MusicPlayerManager", "YouTube Music extraction also failed for: $title")
                
                // Set error state - no sample audio fallback
                _isPlaying.value = false
                _currentSong.value = _currentSong.value?.copy(
                    title = "$title (Unavailable)",
                    artist = "$artist - Stream not available"
                )
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MusicPlayerManager", "All YouTube extraction methods failed for: $title", e)
            
            // Set error state instead of playing any fallback audio
            _isPlaying.value = false
            _currentSong.value = _currentSong.value?.copy(
                title = "$title (Error)",
                artist = "$artist - Unable to stream"
            )
        }
    }

    /**
     * Play a local demo audio track directly  
     * Used for sample tracks that should play reliable background audio
     */
    fun playDemoAudio(song: Song) {
        android.util.Log.d("MusicPlayerManager", "Playing demo audio: ${song.title}")
        
        // Use playSong for demo tracks - these are local and reliable
        playSong(song)
    }

    /** Play a YouTube video via WebView embed instead of extracting streams. */
    fun playYouTube(videoId: String, title: String, artist: String, thumbnailUrl: String?) {
        exoPlayer.stop()
        _currentSong.value = Song(
            id = "yt_$videoId",
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            isLocal = false
        )
        _currentVideoId.value = videoId
        _isPlaying.value = true
        _currentPosition.value = 0L
        _duration.value = 0L
        
        // Try embedded YouTube instead of music.youtube.com (often more permissive)
        val embedUrl = "https://www.youtube.com/embed/$videoId?autoplay=1&controls=0&disablekb=1&fs=0&iv_load_policy=3&modestbranding=1&playsinline=1&rel=0&showinfo=0"
        
        // If WebView already exists, load immediately — otherwise PlayerScreen will do it on first attach
        _webView?.post { _webView?.loadUrl(embedUrl) }
        // Show / update the mini-player notification
        startNotificationService(title, artist, thumbnailUrl)
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0)
        }
        exoPlayer.play()
    }

    fun stop() {
        try {
            exoPlayer.stop()
            _isPlaying.value = false
            _currentPosition.value = 0L
            
            // Stop the notification service properly
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