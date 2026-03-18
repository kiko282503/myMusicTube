package com.musictube.player.viewmodel;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0010\u0002\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\b\u0010 \u001a\u00020\u0012H\u0002J\u0006\u0010!\u001a\u00020\"J\b\u0010#\u001a\u00020\"H\u0002J\b\u0010$\u001a\u00020\"H\u0002J\u000e\u0010%\u001a\u00020\"2\u0006\u0010&\u001a\u00020\u000fJ\u000e\u0010\'\u001a\u00020\"2\u0006\u0010(\u001a\u00020\u001aJ\u0006\u0010)\u001a\u00020\"R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\f\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\r\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00120\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0015R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0015R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0017\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0015R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0019\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u001a0\u000e0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0015R\u000e\u0010\u001c\u001a\u00020\u001dX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u001e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000f0\u000e0\u0014\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010\u0015\u00a8\u0006*"}, d2 = {"Lcom/musictube/player/viewmodel/HomeViewModel;", "Landroidx/lifecycle/ViewModel;", "musicRepository", "Lcom/musictube/player/data/repository/MusicRepository;", "playerManager", "Lcom/musictube/player/service/MusicPlayerManager;", "searchService", "Lcom/musictube/player/service/SearchService;", "(Lcom/musictube/player/data/repository/MusicRepository;Lcom/musictube/player/service/MusicPlayerManager;Lcom/musictube/player/service/SearchService;)V", "_isLoading", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_isLoadingMore", "_quickPicks", "", "Lcom/musictube/player/data/model/SearchResult;", "_trendingSongs", "allTrendingQueries", "", "isLoading", "Lkotlinx/coroutines/flow/StateFlow;", "()Lkotlinx/coroutines/flow/StateFlow;", "isLoadingMore", "quickPicks", "getQuickPicks", "songs", "Lcom/musictube/player/data/model/Song;", "getSongs", "trendingPage", "", "trendingSongs", "getTrendingSongs", "getRandomTrendingQuery", "loadMoreTrending", "", "loadQuickPicks", "loadTrendingSongs", "playSearchResult", "searchResult", "playSong", "song", "reloadTrending", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class HomeViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.musictube.player.data.repository.MusicRepository musicRepository = null;
    @org.jetbrains.annotations.NotNull()
    private final com.musictube.player.service.MusicPlayerManager playerManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.musictube.player.service.SearchService searchService = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.List<java.lang.String> allTrendingQueries = null;
    private int trendingPage = 0;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isLoadingMore = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoadingMore = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.musictube.player.data.model.SearchResult>> _trendingSongs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.musictube.player.data.model.SearchResult>> trendingSongs = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.musictube.player.data.model.SearchResult>> _quickPicks = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.musictube.player.data.model.SearchResult>> quickPicks = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.musictube.player.data.model.Song>> songs = null;
    
    @javax.inject.Inject()
    public HomeViewModel(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.repository.MusicRepository musicRepository, @org.jetbrains.annotations.NotNull()
    com.musictube.player.service.MusicPlayerManager playerManager, @org.jetbrains.annotations.NotNull()
    com.musictube.player.service.SearchService searchService) {
        super();
    }
    
    private final java.lang.String getRandomTrendingQuery() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoading() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isLoadingMore() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.musictube.player.data.model.SearchResult>> getTrendingSongs() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.musictube.player.data.model.SearchResult>> getQuickPicks() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.musictube.player.data.model.Song>> getSongs() {
        return null;
    }
    
    private final void loadTrendingSongs() {
    }
    
    public final void loadMoreTrending() {
    }
    
    private final void loadQuickPicks() {
    }
    
    public final void reloadTrending() {
    }
    
    public final void playSearchResult(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.SearchResult searchResult) {
    }
    
    public final void playSong(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Song song) {
    }
}