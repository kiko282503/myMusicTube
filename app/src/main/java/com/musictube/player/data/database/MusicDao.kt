package com.musictube.player.data.database

import androidx.room.*
import com.musictube.player.data.model.Song
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE isLiked = 1 ORDER BY dateAdded DESC")
    fun getLikedSongs(): Flow<List<Song>>
    
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): Song?
    
    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<Song>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)
    
    @Update
    suspend fun updateSong(song: Song)
    
    @Delete
    suspend fun deleteSong(song: Song)
    
    @Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String)
    
    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikedStatus(songId: String, isLiked: Boolean)
}

@Dao
interface PlaylistDao {
    
    @Query("SELECT * FROM playlists ORDER BY dateCreated DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): Playlist?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    // Get songs in a playlist
    @Query("""
        SELECT s.* FROM songs s 
        INNER JOIN playlist_songs ps ON s.id = ps.songId 
        WHERE ps.playlistId = :playlistId 
        ORDER BY ps.position ASC
    """)
    fun getPlaylistSongs(playlistId: String): Flow<List<Song>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(playlistSong: PlaylistSong)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: String)
}