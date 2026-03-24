package com.musictube.player.data.repository

import android.content.Context
import android.provider.MediaStore
import com.musictube.player.data.database.SongDao
import com.musictube.player.data.database.PlaylistDao
import com.musictube.player.data.model.PlaylistSong
import com.musictube.player.data.model.Song
import com.musictube.player.data.model.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao
) {
    
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    fun getDownloadedSongs(): Flow<List<Song>> = songDao.getDownloadedSongs()

    fun getLikedSongs(): Flow<List<Song>> = songDao.getLikedSongs()
    
    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)
    
    suspend fun getSongById(id: String): Song? = songDao.getSongById(id)
    
    suspend fun insertSong(song: Song) = songDao.insertSong(song)
    
    suspend fun updateLikedStatus(songId: String, isLiked: Boolean) {
        songDao.updateLikedStatus(songId, isLiked)
    }
    
    suspend fun incrementPlayCount(songId: String) {
        songDao.incrementPlayCount(songId)
    }
    
    suspend fun scanLocalMusic() {
        withContext(Dispatchers.IO) {
            val songs = mutableListOf<Song>()
            
            // Query MediaStore for audio files
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn)
                    val duration = cursor.getLong(durationColumn)
                    val filePath = cursor.getString(dataColumn)
                    
                    val song = Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        filePath = filePath,
                        isLocal = true
                    )
                    
                    songs.add(song)
                }
            }
            
            // Insert all songs into database
            songDao.insertSongs(songs)
        }
    }
    
    // Playlist methods
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
        .map { playlists ->
            playlists
                .groupBy { it.name.trim().lowercase(Locale.getDefault()) }
                .values
                .map { sameName ->
                    sameName.maxWithOrNull(
                        compareBy<Playlist>({ it.songCount }, { it.dateCreated })
                    ) ?: sameName.first()
                }
                .sortedByDescending { it.dateCreated }
        }

    fun getPlaylistByIdFlow(playlistId: String): Flow<Playlist?> = playlistDao.getPlaylistByIdFlow(playlistId)
    
    fun getPlaylistSongs(playlistId: String): Flow<List<Song>> = 
        playlistDao.getPlaylistSongs(playlistId)

    fun isSongInAnyPlaylist(songId: String): Flow<Boolean> =
        playlistDao.isSongInAnyPlaylist(songId).map { it > 0 }

    suspend fun createPlaylist(name: String, description: String? = null): String {
        val normalizedName = name.trim()
        val existing = playlistDao.getPlaylistByName(normalizedName)
        if (existing != null) return existing.id

        val playlistId = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = playlistId,
            name = normalizedName,
            description = description
        )
        playlistDao.insertPlaylist(playlist)
        return playlistId
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        // Ensure the song exists in songs table.
        songDao.insertSong(song)
        return addExistingSongToPlaylist(playlistId, song.id)
    }

    suspend fun addExistingSongToPlaylist(playlistId: String, songId: String): Boolean {
        if (playlistDao.isSongInPlaylist(playlistId, songId) > 0) {
            return false
        }

        val nextPosition = (playlistDao.getMaxPositionInPlaylist(playlistId) ?: -1) + 1
        playlistDao.addSongToPlaylist(
            PlaylistSong(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition
            )
        )
        refreshPlaylistCount(playlistId)
        return true
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        refreshPlaylistCount(playlistId)
    }

    suspend fun markSongNotDownloaded(songId: String) {
        songDao.updateDownloadedStatus(songId, isDownloaded = false, filePath = null)
    }

    suspend fun deletePlaylist(playlistId: String) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.clearPlaylist(playlistId)
        playlistDao.deletePlaylist(playlist)
    }

    private suspend fun refreshPlaylistCount(playlistId: String) {
        val count = playlistDao.getPlaylistSongCount(playlistId)
        playlistDao.updatePlaylistSongCount(playlistId, count)
    }
}