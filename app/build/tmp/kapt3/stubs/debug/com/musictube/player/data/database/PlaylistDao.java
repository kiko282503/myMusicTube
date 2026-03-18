package com.musictube.player.data.database;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0006\bg\u0018\u00002\u00020\u0001J\u0016\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u0016\u0010\u0007\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u0014\u0010\u000f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\r0\u00110\u0010H\'J\u0018\u0010\u0012\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0013\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\nJ\u001c\u0010\u0014\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00150\u00110\u00102\u0006\u0010\b\u001a\u00020\tH\'J\u0016\u0010\u0016\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000eJ\u001e\u0010\u0017\u001a\u00020\u00032\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\u0018\u001a\u00020\tH\u00a7@\u00a2\u0006\u0002\u0010\u0019J\u0016\u0010\u001a\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH\u00a7@\u00a2\u0006\u0002\u0010\u000e\u00a8\u0006\u001b"}, d2 = {"Lcom/musictube/player/data/database/PlaylistDao;", "", "addSongToPlaylist", "", "playlistSong", "Lcom/musictube/player/data/model/PlaylistSong;", "(Lcom/musictube/player/data/model/PlaylistSong;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "clearPlaylist", "playlistId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deletePlaylist", "playlist", "Lcom/musictube/player/data/model/Playlist;", "(Lcom/musictube/player/data/model/Playlist;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllPlaylists", "Lkotlinx/coroutines/flow/Flow;", "", "getPlaylistById", "id", "getPlaylistSongs", "Lcom/musictube/player/data/model/Song;", "insertPlaylist", "removeSongFromPlaylist", "songId", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "updatePlaylist", "app_debug"})
@androidx.room.Dao()
public abstract interface PlaylistDao {
    
    @androidx.room.Query(value = "SELECT * FROM playlists ORDER BY dateCreated DESC")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.musictube.player.data.model.Playlist>> getAllPlaylists();
    
    @androidx.room.Query(value = "SELECT * FROM playlists WHERE id = :id")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getPlaylistById(@org.jetbrains.annotations.NotNull()
    java.lang.String id, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.musictube.player.data.model.Playlist> $completion);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object insertPlaylist(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Playlist playlist, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Update()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updatePlaylist(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Playlist playlist, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Delete()
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object deletePlaylist(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.Playlist playlist, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "\n        SELECT s.* FROM songs s \n        INNER JOIN playlist_songs ps ON s.id = ps.songId \n        WHERE ps.playlistId = :playlistId \n        ORDER BY ps.position ASC\n    ")
    @org.jetbrains.annotations.NotNull()
    public abstract kotlinx.coroutines.flow.Flow<java.util.List<com.musictube.player.data.model.Song>> getPlaylistSongs(@org.jetbrains.annotations.NotNull()
    java.lang.String playlistId);
    
    @androidx.room.Insert(onConflict = 1)
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object addSongToPlaylist(@org.jetbrains.annotations.NotNull()
    com.musictube.player.data.model.PlaylistSong playlistSong, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object removeSongFromPlaylist(@org.jetbrains.annotations.NotNull()
    java.lang.String playlistId, @org.jetbrains.annotations.NotNull()
    java.lang.String songId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
    
    @androidx.room.Query(value = "DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object clearPlaylist(@org.jetbrains.annotations.NotNull()
    java.lang.String playlistId, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion);
}