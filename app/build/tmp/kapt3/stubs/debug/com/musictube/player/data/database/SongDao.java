package com.musictube.player.data.database;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u0003\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0014\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\bH\'J\u0014\u0010\n\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\bH\'J\u0018\u0010\u000b\u001a\u0004\u0018\u00010\u00052\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u000f\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0016\u0010\u0011\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001c\u0010\u0012\u001a\u00020\u00032\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00050\tH\u00a7@\u00a2\u0006\u0002\u0010\u0014J\u001c\u0010\u0015\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00050\t0\b2\u0006\u0010\u0016\u001a\u00020\rH\'J\u001e\u0010\u0017\u001a\u00020\u00032\u0006\u0010\u0010\u001a\u00020\r2\u0006\u0010\u0018\u001a\u00020\u0019H\u00a7@\u00a2\u0006\u0002\u0010\u001aJ\u0016\u0010\u001b\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u001c"}, d2 = {"Lcom/musictube/player/data/database/SongDao;", "", "deleteSong", "", "song", "Lcom/musictube/player/data/model/Song;", "(Lcom/musictube/player/data/model/Song;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllSongs", "Lkotlinx/coroutines/flow/Flow;", "", "getLikedSongs", "getSongById", "id", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "incrementPlayCount", "songId", "insertSong", "insertSongs", "songs", "(Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "searchSongs", "query", "updateLikedStatus", "isLiked", "", "(Ljava/lang/String;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updateSong", "app_debug"})
@androidx.room.Dao()
public abstract interface SongDao {
    
    @androidx.room.Query(value = "SELECT * FROM songs ORDER BY dateAdded DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.musictube.player.data.model.Song>> getAllSongs();
    
    @androidx.room.Query(value = "SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.musictube.player.data.model.Song>> getLikedSongs();
    
    @androidx.room.Query(value = "SELECT * FROM songs WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSongById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.musictube.player.data.model.Song> $completion);
    
    @androidx.room.Query(value = "SELECT * FROM songs WHERE title LIKE \'%\' || :query || \'%\' OR artist LIKE \'%\' || :query || \'%\'")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.musictube.player.data.model.Song>> searchSongs(@org.jetbrains.annotations.NotNull()
    java.lang.String query);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertSong(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Song song, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertSongs(@org.jetbrains.annotations.NotNull()
    java.util.List<com.musictube.player.data.model.Song> songs, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateSong(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Song song, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deleteSong(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Song song, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object incrementPlayCount(@org.jetbrains.annotations.NotNull()
    java.lang.String songId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateLikedStatus(@org.jetbrains.annotations.NotNull()
    java.lang.String songId, boolean isLiked, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}