package com.musictube.player.service;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\t\u001a\u0004\u0018\u00010\u00072\u0006\u0010\n\u001a\u00020\u0007H\u0086@\u00a2\u0006\u0002\u0010\u000bJ\u0012\u0010\f\u001a\u0004\u0018\u00010\u00072\u0006\u0010\n\u001a\u00020\u0007H\u0002J\u0012\u0010\r\u001a\u0004\u0018\u00010\u00072\u0006\u0010\n\u001a\u00020\u0007H\u0002J\u0012\u0010\u000e\u001a\u0004\u0018\u00010\u00072\u0006\u0010\n\u001a\u00020\u0007H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/musictube/player/service/YouTubeStreamService;", "", "()V", "httpClient", "Lokhttp3/OkHttpClient;", "invidiousInstances", "", "", "pipedInstances", "extractAudioUrl", "videoId", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "fetchWithCobalt", "fetchWithInvidious", "fetchWithPiped", "app_debug"})
public final class YouTubeStreamService {
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient httpClient = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> pipedInstances = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> invidiousInstances = null;
    
    @javax.inject.Inject()
    public YouTubeStreamService() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object extractAudioUrl(@org.jetbrains.annotations.NotNull()
    java.lang.String videoId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.lang.String> $completion) {
        return null;
    }
    
    private final java.lang.String fetchWithCobalt(java.lang.String videoId) {
        return null;
    }
    
    private final java.lang.String fetchWithPiped(java.lang.String videoId) {
        return null;
    }
    
    private final java.lang.String fetchWithInvidious(java.lang.String videoId) {
        return null;
    }
}