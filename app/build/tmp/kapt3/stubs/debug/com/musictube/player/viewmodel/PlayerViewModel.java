package com.musictube.player.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aJ\b\u0010\u001b\u001a\u00020\u001cH\u0014J\u000e\u0010\u001d\u001a\u00020\u001c2\u0006\u0010\u0019\u001a\u00020\u001aJ\u0006\u0010\u001e\u001a\u00020\u001cJ\u0006\u0010\u001f\u001a\u00020\u001cJ\u0006\u0010 \u001a\u00020\u001cJ\u0006\u0010!\u001a\u00020\u001cJ\u000e\u0010\"\u001a\u00020\u001c2\u0006\u0010#\u001a\u00020\u0007J\u000e\u0010$\u001a\u00020\u001c2\u0006\u0010\u0014\u001a\u00020\u0015J\u0006\u0010%\u001a\u00020\u001cR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\tR\u0019\u0010\n\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000b0\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\tR\u0019\u0010\r\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u000e0\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\tR\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\tR\u0017\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00130\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\tR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00150\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\t\u00a8\u0006&"}, d2 = {"Lcom/musictube/player/viewmodel/PlayerViewModel;", "Landroidx/lifecycle/ViewModel;", "playerManager", "Lcom/musictube/player/service/MusicPlayerManager;", "(Lcom/musictube/player/service/MusicPlayerManager;)V", "currentPosition", "Lkotlinx/coroutines/flow/StateFlow;", "", "getCurrentPosition", "()Lkotlinx/coroutines/flow/StateFlow;", "currentSong", "Lcom/musictube/player/data/model/Song;", "getCurrentSong", "currentVideoId", "", "getCurrentVideoId", "duration", "getDuration", "isPlaying", "", "volume", "", "getVolume", "getOrCreateWebView", "Landroid/webkit/WebView;", "context", "Landroid/content/Context;", "onCleared", "", "parkWebView", "pause", "play", "resume", "resumeWebView", "seekTo", "position", "setVolume", "stop", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class PlayerViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.musictube.player.service.MusicPlayerManager playerManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.musictube.player.data.model.Song> currentSong = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> currentVideoId = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isPlaying = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> currentPosition = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Long> duration = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Float> volume = null;
    
    @javax.inject.Inject()
    public PlayerViewModel(@org.jetbrains.annotations.NotNull()
    com.musictube.player.service.MusicPlayerManager playerManager) {
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
    
    /**
     * Returns the singleton WebView from MusicPlayerManager — creates it on first call.
     */
    @org.jetbrains.annotations.NotNull()
    public final android.webkit.WebView getOrCreateWebView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    /**
     * Call when the app comes back to foreground so YouTube Music resumes if paused.
     */
    public final void resumeWebView() {
    }
    
    /**
     * Park the WebView in the decor view so audio keeps playing when navigating back.
     */
    public final void parkWebView(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
    }
    
    public final void play() {
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
    
    @java.lang.Override()
    protected void onCleared() {
    }
}