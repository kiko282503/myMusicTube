package com.musictube.player.service;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0014\b\u0007\u0018\u00002\u00020\u0001:\u0001GB!\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u001d\u001a\u00020\u000bJ\u0006\u0010$\u001a\u00020\u000bJ\u0010\u00100\u001a\u00020\u001a2\u0006\u0010\u0002\u001a\u00020\u0003H\u0007J\f\u00101\u001a\b\u0012\u0004\u0012\u00020\r02J\b\u00103\u001a\u000204H\u0002J\u000e\u00105\u001a\u0002042\u0006\u0010\u0002\u001a\u00020\u0003J\u0006\u00106\u001a\u000204J\u000e\u00107\u001a\u0002042\u0006\u00108\u001a\u00020\rJ(\u00109\u001a\u0002042\u0006\u0010:\u001a\u00020\u000f2\u0006\u0010;\u001a\u00020\u000f2\u0006\u0010<\u001a\u00020\u000f2\b\u0010=\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010>\u001a\u000204J\u0006\u0010?\u001a\u000204J\u0006\u0010@\u001a\u000204J\u0006\u0010A\u001a\u000204J\u000e\u0010B\u001a\u0002042\u0006\u0010C\u001a\u00020\u000bJ\u000e\u0010D\u001a\u0002042\u0006\u0010.\u001a\u00020\u0018J \u0010E\u001a\u0002042\u0006\u0010;\u001a\u00020\u000f2\u0006\u0010<\u001a\u00020\u000f2\b\u0010=\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010F\u001a\u000204R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0016\u001a\u0004\u0018\u00010\u000fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00180\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0019\u001a\u0004\u0018\u00010\u001aX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u000b0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001eR\u0019\u0010\u001f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001eR\u0019\u0010!\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010\u001eR\u0017\u0010#\u001a\b\u0012\u0004\u0012\u00020\u000b0\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u001eR\u001b\u0010%\u001a\u00020&8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b)\u0010*\u001a\u0004\b\'\u0010(R\u0017\u0010+\u001a\b\u0012\u0004\u0012\u00020\u00120\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001eR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010,\u001a\u00020-X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010.\u001a\b\u0012\u0004\u0012\u00020\u00180\u001c\u00a2\u0006\b\n\u0000\u001a\u0004\b/\u0010\u001e\u00a8\u0006H"}, d2 = {"Lcom/musictube/player/service/MusicPlayerManager;", "", "context", "Landroid/content/Context;", "audioExtractor", "Lcom/musictube/player/service/YouTubeAudioExtractor;", "localAudioManager", "Lcom/musictube/player/service/LocalAudioManager;", "(Landroid/content/Context;Lcom/musictube/player/service/YouTubeAudioExtractor;Lcom/musictube/player/service/LocalAudioManager;)V", "_currentPosition", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_currentSong", "Lcom/musictube/player/data/model/Song;", "_currentVideoId", "", "_duration", "_isPlaying", "", "_lastArtist", "_lastThumb", "_lastTitle", "_lastVideoId", "_volume", "", "_webView", "Landroid/webkit/WebView;", "currentPosition", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentPosition", "()Lkotlinx/coroutines/flow/StateFlow;", "currentSong", "getCurrentSong", "currentVideoId", "getCurrentVideoId", "duration", "getDuration", "exoPlayer", "Landroidx/media3/exoplayer/ExoPlayer;", "getExoPlayer", "()Landroidx/media3/exoplayer/ExoPlayer;", "exoPlayer$delegate", "Lkotlin/Lazy;", "isPlaying", "scope", "Lkotlinx/coroutines/CoroutineScope;", "volume", "getVolume", "getOrCreateWebView", "getSampleSongs", "", "injectVideoMonitor", "", "parkWebView", "pause", "playSong", "song", "playYouTubeAudioStream", "videoId", "title", "artist", "thumbnailUrl", "release", "replayCurrent", "resume", "resumeWebView", "seekTo", "position", "setVolume", "startNotificationService", "stop", "YouTubeInterface", "app_debug"})
public final class MusicPlayerManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.musictube.player.service.YouTubeAudioExtractor audioExtractor = null;
    @org.jetbrains.annotations.NotNull()
    private final com.musictube.player.service.LocalAudioManager localAudioManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.musictube.player.data.model.Song> _currentSong = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.musictube.player.data.model.Song> currentSong = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _currentVideoId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentVideoId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _currentPosition = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> currentPosition = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Long> _duration = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> duration = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Float> _volume = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Float> volume = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy exoPlayer$delegate = null;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String _lastVideoId;
    @org.jetbrains.annotations.NotNull()
    private java.lang.String _lastTitle = "";
    @org.jetbrains.annotations.NotNull()
    private java.lang.String _lastArtist = "";
    @org.jetbrains.annotations.Nullable()
    private java.lang.String _lastThumb;
    @org.jetbrains.annotations.Nullable()
    private android.webkit.WebView _webView;
    
    @javax.inject.Inject()
    public MusicPlayerManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.musictube.player.service.YouTubeAudioExtractor audioExtractor, @org.jetbrains.annotations.NotNull()
    com.musictube.player.service.LocalAudioManager localAudioManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.musictube.player.data.model.Song> getCurrentSong() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getCurrentVideoId() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getCurrentPosition() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Long> getDuration() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Float> getVolume() {
        return null;
    }
    
    private final androidx.media3.exoplayer.ExoPlayer getExoPlayer() {
        return null;
    }
    
    public final void playSong(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Song song) {
    }
    
    /**
     * Returns the singleton WebView. When called with an Activity context and the WebView
     * is not yet in a window, attaches it to android.R.id.content (VISIBLE 1×1px) so
     * autoplay policy is satisfied — without ever moving it between parents.
     */
    @android.annotation.SuppressLint(value = {"SetJavaScriptEnabled"})
    @org.jetbrains.annotations.NotNull()
    public final android.webkit.WebView getOrCreateWebView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    /**
     * Injects a JS poller into the WebView that watches the <video> element on
     * music.youtube.com and bridges play/pause/ended/timeupdate events back to Android.
     */
    private final void injectVideoMonitor() {
    }
    
    /**
     * Replay the current song from the beginning.
     * For YouTube: if the page is already loaded for this videoId, seek to 0 and play via JS
     * (avoids a full page reload which triggers autoplay policy rejection).
     * For local tracks: restart via ExoPlayer.
     */
    public final void replayCurrent() {
    }
    
    /**
     * Resume the WebView timers when app returns to foreground.
     */
    public final void resumeWebView() {
    }
    
    /**
     * Called when the app goes to background. The WebView stays in android.R.id.content
     * (no view hierarchy change) — just keep timers running and spoof visibility.
     */
    public final void parkWebView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    /**
     * Start / refresh the foreground notification service for the current song.
     */
    public final void startNotificationService(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, @org.jetbrains.annotations.Nullable()
    java.lang.String thumbnailUrl) {
    }
    
    /**
     * Play a YouTube song by loading the full music.youtube.com page in the WebView.
     * No embed restrictions, no extraction needed — same as opening in a browser.
     * JS injected on pageFinished monitors the <video> element and bridges state back.
     */
    public final void playYouTubeAudioStream(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, @org.jetbrains.annotations.Nullable()
    java.lang.String thumbnailUrl) {
    }
    
    public final void pause() {
    }
    
    public final void resume() {
    }
    
    public final void stop() {
    }
    
    public final void seekTo(long position) {
    }
    
    public final void setVolume(float volume) {
    }
    
    public final void release() {
    }
    
    public final long getCurrentPosition() {
        return 0L;
    }
    
    public final long getDuration() {
        return 0L;
    }
    
    /**
     * Get sample audio files that demonstrate working background playback
     */
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.musictube.player.data.model.Song> getSampleSongs() {
        return null;
    }
    
    /**
     * JavaScript interface bridging <video> element events into our StateFlows.
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0007\b\u0086\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\b\u0010\u0007\u001a\u00020\u0004H\u0007J\b\u0010\b\u001a\u00020\u0004H\u0007J\u0010\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0018\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\b\u0010\f\u001a\u00020\u0004H\u0007\u00a8\u0006\r"}, d2 = {"Lcom/musictube/player/service/MusicPlayerManager$YouTubeInterface;", "", "(Lcom/musictube/player/service/MusicPlayerManager;)V", "onDuration", "", "durationMs", "", "onEnded", "onPaused", "onPlaying", "onPosition", "positionMs", "onVideoError", "app_debug"})
    public final class YouTubeInterface {
        
        public YouTubeInterface() {
            super();
        }
        
        @android.webkit.JavascriptInterface()
        public final void onPlaying(double durationMs) {
        }
        
        @android.webkit.JavascriptInterface()
        public final void onPaused() {
        }
        
        @android.webkit.JavascriptInterface()
        public final void onEnded() {
        }
        
        @android.webkit.JavascriptInterface()
        public final void onDuration(double durationMs) {
        }
        
        @android.webkit.JavascriptInterface()
        public final void onPosition(double positionMs, double durationMs) {
        }
        
        @android.webkit.JavascriptInterface()
        public final void onVideoError() {
        }
    }
}