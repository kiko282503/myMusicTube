package com.musictube.player.service;

@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\t\n\u0002\b\u0004\b\u0007\u0018\u0000 62\u00020\u0001:\u00016B\u0005\u00a2\u0006\u0002\u0010\u0002J*\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\n2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\bH\u0002J\b\u0010\u001e\u001a\u00020\u001fH\u0002J\u0014\u0010 \u001a\u0004\u0018\u00010!2\b\u0010\"\u001a\u0004\u0018\u00010#H\u0016J\b\u0010$\u001a\u00020\u001fH\u0016J\b\u0010%\u001a\u00020\u001fH\u0016J\"\u0010&\u001a\u00020\'2\b\u0010\"\u001a\u0004\u0018\u00010#2\u0006\u0010(\u001a\u00020\'2\u0006\u0010)\u001a\u00020\'H\u0016J\u0018\u0010*\u001a\u00020+2\u0006\u0010,\u001a\u00020\n2\u0006\u0010-\u001a\u00020\'H\u0002J(\u0010.\u001a\u00020\u001f2\u0006\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\n2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010/\u001a\u0004\u0018\u00010\nJ*\u00100\u001a\u00020\u001f2\u0006\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\n2\u0006\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\bH\u0002J:\u00101\u001a\u00020\u001f2\u0006\u0010\u0019\u001a\u00020\n2\u0006\u0010\u001a\u001a\u00020\n2\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u00102\u001a\u0002032\u0006\u00104\u001a\u0002032\b\u0010\u001d\u001a\u0004\u0018\u00010\bH\u0002J\u0010\u00105\u001a\u00020\u001f2\u0006\u0010\u001b\u001a\u00020\u001cH\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082.\u00a2\u0006\u0002\n\u0000R\u001e\u0010\r\u001a\u00020\u000e8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00067"}, d2 = {"Lcom/musictube/player/service/MusicPlayerService;", "Landroid/app/Service;", "()V", "audioFocusRequest", "Landroid/media/AudioFocusRequest;", "audioManager", "Landroid/media/AudioManager;", "cachedArt", "Landroid/graphics/Bitmap;", "cachedArtUrl", "", "mediaSession", "Landroid/support/v4/media/session/MediaSessionCompat;", "playerManager", "Lcom/musictube/player/service/MusicPlayerManager;", "getPlayerManager", "()Lcom/musictube/player/service/MusicPlayerManager;", "setPlayerManager", "(Lcom/musictube/player/service/MusicPlayerManager;)V", "scope", "Lkotlinx/coroutines/CoroutineScope;", "thumbnailJob", "Lkotlinx/coroutines/Job;", "buildNotification", "Landroid/app/Notification;", "title", "artist", "isPlaying", "", "art", "createNotificationChannel", "", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onStartCommand", "", "flags", "startId", "pendingServiceIntent", "Landroid/app/PendingIntent;", "action", "requestCode", "showNotification", "thumbnailUrl", "updateMediaSession", "updateMediaSessionWithPosition", "position", "", "duration", "updateNotificationPlayState", "Companion", "app_debug"})
public final class MusicPlayerService extends android.app.Service {
    @javax.inject.Inject()
    public com.musictube.player.service.MusicPlayerManager playerManager;
    private android.support.v4.media.session.MediaSessionCompat mediaSession;
    private android.media.AudioManager audioManager;
    @org.jetbrains.annotations.Nullable()
    private android.media.AudioFocusRequest audioFocusRequest;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable()
    private kotlinx.coroutines.Job thumbnailJob;
    @org.jetbrains.annotations.Nullable()
    private android.graphics.Bitmap cachedArt;
    @org.jetbrains.annotations.Nullable()
    private java.lang.String cachedArtUrl;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String CHANNEL_ID = "music_channel";
    public static final int NOTIFICATION_ID = 1;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_PLAY = "com.musictube.ACTION_PLAY";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_PAUSE = "com.musictube.ACTION_PAUSE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_STOP = "com.musictube.ACTION_STOP";
    @org.jetbrains.annotations.NotNull()
    public static final com.musictube.player.service.MusicPlayerService.Companion Companion = null;
    
    public MusicPlayerService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.musictube.player.service.MusicPlayerManager getPlayerManager() {
        return null;
    }
    
    public final void setPlayerManager(@org.jetbrains.annotations.NotNull()
    com.musictube.player.service.MusicPlayerManager p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @java.lang.Override()
    public int onStartCommand(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent, int flags, int startId) {
        return 0;
    }
    
    /**
     * Called by MusicPlayerManager to show/update the notification for a new song.
     */
    public final void showNotification(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String artist, boolean isPlaying, @org.jetbrains.annotations.Nullable()
    java.lang.String thumbnailUrl) {
    }
    
    private final void updateMediaSession(java.lang.String title, java.lang.String artist, boolean isPlaying, android.graphics.Bitmap art) {
    }
    
    private final void updateMediaSessionWithPosition(java.lang.String title, java.lang.String artist, boolean isPlaying, long position, long duration, android.graphics.Bitmap art) {
    }
    
    private final void updateNotificationPlayState(boolean isPlaying) {
    }
    
    private final android.app.Notification buildNotification(java.lang.String title, java.lang.String artist, boolean isPlaying, android.graphics.Bitmap art) {
        return null;
    }
    
    private final android.app.PendingIntent pendingServiceIntent(java.lang.String action, int requestCode) {
        return null;
    }
    
    private final void createNotificationChannel() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public android.os.IBinder onBind(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent) {
        return null;
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\n"}, d2 = {"Lcom/musictube/player/service/MusicPlayerService$Companion;", "", "()V", "ACTION_PAUSE", "", "ACTION_PLAY", "ACTION_STOP", "CHANNEL_ID", "NOTIFICATION_ID", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}