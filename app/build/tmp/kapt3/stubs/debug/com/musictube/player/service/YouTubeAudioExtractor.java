package com.musictube.player.service;

/**
 * Extracts YouTube audio stream URLs via the Piped API (open-source YouTube proxy).
 * Piped does server-side extraction and returns usable audio stream URLs.
 * Multiple instances are tried in order for reliability.
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\b\u0007\u0018\u0000 \f2\u00020\u0001:\u0001\fB\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\u0005\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\bJ\u0018\u0010\t\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u0007\u001a\u00020\u0006H\u0086@\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\n\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u000b\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\u0006H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\r"}, d2 = {"Lcom/musictube/player/service/YouTubeAudioExtractor;", "", "()V", "client", "Lokhttp3/OkHttpClient;", "extractAudioUrl", "", "videoId", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "extractFromYouTubeMusic", "fetchFromPiped", "instance", "Companion", "app_debug"})
public final class YouTubeAudioExtractor {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String TAG = "YouTubeAudioExtractor";
    @org.jetbrains.annotations.NotNull()
    private static final java.util.List<java.lang.String> PIPED_INSTANCES = null;
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient client = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.musictube.player.service.YouTubeAudioExtractor.Companion Companion = null;
    
    @javax.inject.Inject()
    public YouTubeAudioExtractor() {
        super();
    }
    
    /**
     * Extract best audio stream URL for a YouTube video ID.
     * Tries multiple Piped API instances until one succeeds.
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object extractAudioUrl(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    /**
     * Same as extractAudioUrl - Piped handles YouTube Music video IDs too
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object extractFromYouTubeMusic(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.String fetchFromPiped(java.lang.String instance, java.lang.String videoId) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/musictube/player/service/YouTubeAudioExtractor$Companion;", "", "()V", "PIPED_INSTANCES", "", "", "TAG", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}