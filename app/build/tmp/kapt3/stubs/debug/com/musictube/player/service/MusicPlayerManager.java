package com.musictube.player.service;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0000\n\u0002\u0010\u0002\n\u0002\b\u001a\b\u0007\u0018\u00002\u00020\u0001B!\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0019\u001a\u00020\u000bJ\u0006\u0010 \u001a\u00020\u000bJ\u0010\u0010,\u001a\u00020\u00162\u0006\u0010\u0002\u001a\u00020\u0003H\u0007J\f\u0010-\u001a\b\u0012\u0004\u0012\u00020\r0.J\u000e\u0010/\u001a\u0002002\u0006\u0010\u0002\u001a\u00020\u0003J\u0006\u00101\u001a\u000200J\u0006\u00102\u001a\u000200J\u000e\u00103\u001a\u0002002\u0006\u00104\u001a\u00020\rJ2\u00105\u001a\u0002002\u0006\u00106\u001a\u00020\u000f2\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u000f2\u0006\u0010:\u001a\u00020\u000fH\u0002J*\u0010;\u001a\u0002002\u0006\u00106\u001a\u00020\u000f2\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u000fH\u0002J\u000e\u0010<\u001a\u0002002\u0006\u00104\u001a\u00020\rJ(\u0010=\u001a\u0002002\u0006\u00106\u001a\u00020\u000f2\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u000fJ(\u0010>\u001a\u0002002\u0006\u00106\u001a\u00020\u000f2\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010?\u001a\u000200J\u0006\u0010@\u001a\u000200J\u0006\u0010A\u001a\u000200J\u0006\u0010B\u001a\u000200J0\u0010C\u001a\u0002002\u0006\u00106\u001a\u00020\u000f2\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u000fH\u0082@\u00a2\u0006\u0002\u0010DJ\u000e\u0010E\u001a\u0002002\u0006\u0010F\u001a\u00020\u000bJ\u000e\u0010G\u001a\u0002002\u0006\u0010*\u001a\u00020\u0014J \u0010H\u001a\u0002002\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\b\u00109\u001a\u0004\u0018\u00010\u000fJ\u0006\u0010I\u001a\u000200R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\f\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00140\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0019\u0010\u001b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\r0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001aR\u0019\u0010\u001d\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000f0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001aR\u0017\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001aR\u001b\u0010!\u001a\u00020\"8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b%\u0010&\u001a\u0004\b#\u0010$R\u0017\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00120\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u001aR\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010(\u001a\u00020)X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010*\u001a\b\u0012\u0004\u0012\u00020\u00140\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u001a\u00a8\u0006J"}, d2 = {"Lcom/musictube/player/service/MusicPlayerManager;", "", "context", "Landroid/content/Context;", "audioExtractor", "Lcom/musictube/player/service/YouTubeAudioExtractor;", "localAudioManager", "Lcom/musictube/player/service/LocalAudioManager;", "(Landroid/content/Context;Lcom/musictube/player/service/YouTubeAudioExtractor;Lcom/musictube/player/service/LocalAudioManager;)V", "_currentPosition", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_currentSong", "Lcom/musictube/player/data/model/Song;", "_currentVideoId", "", "_duration", "_isPlaying", "", "_volume", "", "_webView", "Landroid/webkit/WebView;", "currentPosition", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentPosition", "()Lkotlinx/coroutines/flow/StateFlow;", "currentSong", "getCurrentSong", "currentVideoId", "getCurrentVideoId", "duration", "getDuration", "exoPlayer", "Landroidx/media3/exoplayer/ExoPlayer;", "getExoPlayer", "()Landroidx/media3/exoplayer/ExoPlayer;", "exoPlayer$delegate", "Lkotlin/Lazy;", "isPlaying", "scope", "Lkotlinx/coroutines/CoroutineScope;", "volume", "getVolume", "getOrCreateWebView", "getSampleSongs", "", "parkWebView", "", "pause", "pauseWebViewPlayback", "playDemoAudio", "song", "playDirectAudioStream", "videoId", "title", "artist", "thumbnailUrl", "audioUrl", "playSampleAudioFallback", "playSong", "playYouTube", "playYouTubeAudioStream", "release", "resume", "resumeWebView", "resumeWebViewPlayback", "retryYouTubeExtraction", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "seekTo", "position", "setVolume", "startNotificationService", "stop", "app_debug"})
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
     * Returns the singleton WebView, creating it with ApplicationContext if needed.
     */
    @android.annotation.SuppressLint(value = {"SetJavaScriptEnabled"})
    @org.jetbrains.annotations.NotNull()
    public final android.webkit.WebView getOrCreateWebView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    /**
     * Call this when the app returns to foreground so YouTube Music resumes if it was paused.
     */
    public final void resumeWebView() {
    }
    
    /**
     * Moves the WebView into the Activity's decor view so it stays attached to a window
     * while Compose navigates away from PlayerScreen — audio keeps playing uninterrupted.
     */
    public final void parkWebView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    /**
     * Pause YouTube Music playback by injecting JS into the WebView.
     */
    public final void pauseWebViewPlayback() {
    }
    
    /**
     * Resume YouTube Music playback by injecting JS into the WebView.
     */
    public final void resumeWebViewPlayback() {
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
     * Play using ExoPlayer only - always extract real YouTube audio, no fallbacks
     */
    public final void playYouTubeAudioStream(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId, @org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, @org.jetbrains.annotations.Nullable()
    java.lang.String thumbnailUrl) {
    }
    
    private final void playDirectAudioStream(java.lang.String videoId, java.lang.String title, java.lang.String artist, java.lang.String thumbnailUrl, java.lang.String audioUrl) {
    }
    
    /**
     * Fallback to sample audio when YouTube extraction fails - starts immediately
     */
    private final void playSampleAudioFallback(java.lang.String videoId, java.lang.String title, java.lang.String artist, java.lang.String thumbnailUrl) {
    }
    
    /**
     * Retry YouTube extraction with alternative methods
     */
    private final java.lang.Object retryYouTubeExtraction(java.lang.String videoId, java.lang.String title, java.lang.String artist, java.lang.String thumbnailUrl, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Play a local demo audio track directly  
     * Used for sample tracks that should play reliable background audio
     */
    public final void playDemoAudio(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Song song) {
    }
    
    /**
     * Play a YouTube video via WebView embed instead of extracting streams.
     */
    public final void playYouTube(@org.jetbrains.annotations.NotNull()
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
}