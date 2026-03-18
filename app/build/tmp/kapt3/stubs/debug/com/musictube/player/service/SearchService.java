package com.musictube.player.service;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u001e\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nH\u0002J\u0012\u0010\f\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\r\u001a\u00020\u000eH\u0002J&\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00102\u0006\u0010\u0011\u001a\u00020\u00122\b\b\u0002\u0010\u0013\u001a\u00020\u0014H\u0086@\u00a2\u0006\u0002\u0010\u0015J\u001e\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000b0\u00102\u0006\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0014H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0017"}, d2 = {"Lcom/musictube/player/service/SearchService;", "", "()V", "httpClient", "Lokhttp3/OkHttpClient;", "findMusicListItemRenderers", "", "element", "Lcom/google/gson/JsonElement;", "results", "", "Lcom/musictube/player/data/model/SearchResult;", "parseMusicListItemRenderer", "renderer", "Lcom/google/gson/JsonObject;", "searchMusic", "", "query", "", "songsOnly", "", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "searchYouTube", "app_debug"})
public final class SearchService {
    @org.jetbrains.annotations.NotNull()
    private final okhttp3.OkHttpClient httpClient = null;
    
    @javax.inject.Inject()
    public SearchService() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object searchMusic(@org.jetbrains.annotations.NotNull()
    java.lang.String query, boolean songsOnly, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<com.musictube.player.data.model.SearchResult>> $completion) {
        return null;
    }
    
    private final java.util.List<com.musictube.player.data.model.SearchResult> searchYouTube(java.lang.String query, boolean songsOnly) {
        return null;
    }
    
    private final void findMusicListItemRenderers(com.google.gson.JsonElement element, java.util.List<com.musictube.player.data.model.SearchResult> results) {
    }
    
    private final com.musictube.player.data.model.SearchResult parseMusicListItemRenderer(com.google.gson.JsonObject renderer) {
        return null;
    }
}