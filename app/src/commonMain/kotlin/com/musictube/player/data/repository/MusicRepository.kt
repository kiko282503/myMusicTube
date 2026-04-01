package com.musictube.player.data.repository

import com.musictube.player.data.database.DatabaseHelper
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.PlaylistSong
import com.musictube.player.data.model.Song
import com.musictube.player.platform.currentTimeMillis
import com.musictube.player.platform.platformUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(private val db: DatabaseHelper) {

    // ── Songs ──────────────────────────────────────────────────────────────────

    fun getAllSongs(): Flow<List<Song>> = db.getAllSongs()

    fun getDownloadedSongs(): Flow<List<Song>> = db.getDownloadedSongs()

    fun getLikedSongs(): Flow<List<Song>> = db.getLikedSongs()

    fun searchSongs(query: String): Flow<List<Song>> = db.searchSongs("%$query%")

    suspend fun getSongById(id: String): Song? = db.getSongById(id)

    suspend fun insertSong(song: Song) = db.insertSong(
        song.copy(dateAdded = if (song.dateAdded == 0L) currentTimeMillis() else song.dateAdded)
    )

    suspend fun updateLikedStatus(songId: String, isLiked: Boolean) =
        db.updateLikedStatus(songId, isLiked)

    suspend fun updateDownloadedStatus(songId: String, isDownloaded: Boolean, filePath: String?) =
        db.updateDownloadedStatus(songId, isDownloaded, filePath)

    suspend fun markSongNotDownloaded(songId: String) =
        db.updateDownloadedStatus(songId, isDownloaded = false, filePath = null)

    suspend fun incrementPlayCount(songId: String) = db.incrementPlayCount(songId)

    // ── Playlists ──────────────────────────────────────────────────────────────

    /** Returns deduplicated playlists sorted newest-first. */
    fun getAllPlaylists(): Flow<List<Playlist>> = db.getAllPlaylists().map { playlists ->
        playlists
            .groupBy { it.name.trim().lowercase() }
            .values
            .map { group ->
                group.maxWithOrNull(compareBy({ it.songCount }, { it.dateCreated })) ?: group.first()
            }
            .sortedByDescending { it.dateCreated }
    }

    fun getPlaylistByIdFlow(id: String): Flow<Playlist?> = db.getPlaylistByIdFlow(id)

    fun getPlaylistSongs(playlistId: String): Flow<List<Song>> = db.getPlaylistSongs(playlistId)

    fun isSongInAnyPlaylist(songId: String): Flow<Boolean> = db.isSongInAnyPlaylist(songId)

    /** Creates the playlist if it does not exist; returns the playlist ID. */
    suspend fun createPlaylist(name: String, description: String? = null): String {
        val normalized = name.trim()
        val existing = db.getPlaylistByName(normalized)
        if (existing != null) return existing.id
        val id = platformUuid()
        db.insertPlaylist(
            Playlist(id = id, name = normalized, description = description,
                dateCreated = currentTimeMillis())
        )
        return id
    }

    suspend fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        insertSong(song)
        return addExistingSongToPlaylist(playlistId, song.id)
    }

    suspend fun addExistingSongToPlaylist(playlistId: String, songId: String): Boolean {
        if (db.isSongInPlaylist(playlistId, songId)) return false
        val position = db.getMaxPositionInPlaylist(playlistId) + 1
        db.addPlaylistSong(
            PlaylistSong(playlistId = playlistId, songId = songId,
                position = position, dateAdded = currentTimeMillis())
        )
        refreshPlaylistCount(playlistId)
        return true
    }

    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        db.removePlaylistSong(playlistId, songId)
        refreshPlaylistCount(playlistId)
    }

    suspend fun deletePlaylist(playlistId: String) = db.deletePlaylist(playlistId)

    suspend fun renamePlaylist(playlistId: String, newName: String) {
        val playlist = db.getPlaylistById(playlistId) ?: return
        db.updatePlaylist(playlist.copy(name = newName))
    }

    suspend fun refreshPlaylistCount(playlistId: String) {
        val count = db.getPlaylistSongCount(playlistId)
        db.updatePlaylistSongCount(playlistId, count)
    }
}
