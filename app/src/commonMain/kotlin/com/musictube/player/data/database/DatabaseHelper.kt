package com.musictube.player.data.database

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.PlaylistSong
import com.musictube.player.data.model.Song
import com.musictube.player.platform.DatabaseDriverFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Thin wrapper around the SQLDelight-generated [MusicDatabase].
 * Provides type-safe Flow queries and suspend mutations mapped to domain models.
 */
class DatabaseHelper(driverFactory: DatabaseDriverFactory) {

    private val db = MusicDatabase(driverFactory.createDriver())
    private val q  = db.musicDatabaseQueries

    // ── Songs ──────────────────────────────────────────────────────────────────

    fun getAllSongs(): Flow<List<Song>> =
        q.getAllSongs().asFlow().mapToList(Dispatchers.IO).map { it.map(Songs::toSong) }

    fun getDownloadedSongs(): Flow<List<Song>> =
        q.getDownloadedSongs().asFlow().mapToList(Dispatchers.IO).map { it.map(Songs::toSong) }

    fun getLikedSongs(): Flow<List<Song>> =
        q.getLikedSongs().asFlow().mapToList(Dispatchers.IO).map { it.map(Songs::toSong) }

    fun searchSongs(query: String): Flow<List<Song>> =
        q.searchSongs(query).asFlow().mapToList(Dispatchers.IO).map { it.map(Songs::toSong) }

    suspend fun getSongById(id: String): Song? = withContext(Dispatchers.IO) {
        q.getSongById(id).executeAsOneOrNull()?.toSong()
    }

    suspend fun insertSong(song: Song) = withContext(Dispatchers.IO) {
        q.insertSong(
            id           = song.id,
            title        = song.title,
            artist       = song.artist,
            album        = song.album,
            duration     = song.duration,
            filePath     = song.filePath,
            url          = song.url,
            thumbnailUrl = song.thumbnailUrl,
            isLocal      = if (song.isLocal) 1L else 0L,
            isDownloaded = if (song.isDownloaded) 1L else 0L,
            dateAdded    = song.dateAdded,
            playCount    = song.playCount.toLong(),
            isLiked      = if (song.isLiked) 1L else 0L
        )
    }

    suspend fun updateLikedStatus(songId: String, isLiked: Boolean) = withContext(Dispatchers.IO) {
        q.updateLikedStatus(if (isLiked) 1L else 0L, songId)
    }

    suspend fun updateDownloadedStatus(songId: String, isDownloaded: Boolean, filePath: String?) =
        withContext(Dispatchers.IO) {
            q.updateDownloadedStatus(if (isDownloaded) 1L else 0L, filePath, songId)
        }

    suspend fun incrementPlayCount(songId: String) = withContext(Dispatchers.IO) {
        q.incrementPlayCount(songId)
    }

    // ── Playlists ──────────────────────────────────────────────────────────────

    fun getAllPlaylists(): Flow<List<Playlist>> =
        q.getAllPlaylists().asFlow().mapToList(Dispatchers.IO).map { it.map(Playlists::toPlaylist) }

    fun getPlaylistByIdFlow(playlistId: String): Flow<Playlist?> =
        q.getPlaylistById(playlistId).asFlow().mapToOneOrNull(Dispatchers.IO).map { it?.toPlaylist() }

    suspend fun getPlaylistById(playlistId: String): Playlist? = withContext(Dispatchers.IO) {
        q.getPlaylistById(playlistId).executeAsOneOrNull()?.toPlaylist()
    }

    suspend fun getPlaylistByName(name: String): Playlist? = withContext(Dispatchers.IO) {
        q.getPlaylistByName(name).executeAsOneOrNull()?.toPlaylist()
    }

    suspend fun insertPlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        q.insertPlaylist(
            id           = playlist.id,
            name         = playlist.name,
            description  = playlist.description,
            thumbnailUrl = playlist.thumbnailUrl,
            dateCreated  = playlist.dateCreated,
            songCount    = playlist.songCount.toLong()
        )
    }

    suspend fun updatePlaylist(playlist: Playlist) = withContext(Dispatchers.IO) {
        q.updatePlaylist(
            name         = playlist.name,
            description  = playlist.description,
            thumbnailUrl = playlist.thumbnailUrl,
            songCount    = playlist.songCount.toLong(),
            id           = playlist.id
        )
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        q.clearPlaylist(playlistId)
        q.deletePlaylist(playlistId)
    }

    // ── Playlist Songs ─────────────────────────────────────────────────────────

    fun getPlaylistSongs(playlistId: String): Flow<List<Song>> =
        q.getPlaylistSongs(playlistId).asFlow().mapToList(Dispatchers.IO)
            .map { it.map(Songs::toSong) }

    fun isSongInAnyPlaylist(songId: String): Flow<Boolean> =
        q.isSongInAnyPlaylist(songId).asFlow().mapToOneOrNull(Dispatchers.IO)
            .map { (it ?: 0L) > 0L }

    suspend fun isSongInPlaylist(playlistId: String, songId: String): Boolean =
        withContext(Dispatchers.IO) {
            (q.isSongInPlaylist(playlistId, songId).executeAsOne()) > 0L
        }

    suspend fun addPlaylistSong(ps: PlaylistSong) = withContext(Dispatchers.IO) {
        q.insertPlaylistSong(ps.playlistId, ps.songId, ps.position.toLong(), ps.dateAdded)
    }

    suspend fun removePlaylistSong(playlistId: String, songId: String) = withContext(Dispatchers.IO) {
        q.deletePlaylistSong(playlistId, songId)
    }

    suspend fun getPlaylistSongCount(playlistId: String): Int = withContext(Dispatchers.IO) {
        q.getPlaylistSongCount(playlistId).executeAsOne().toInt()
    }

    suspend fun getMaxPositionInPlaylist(playlistId: String): Int = withContext(Dispatchers.IO) {
        (q.getMaxPositionInPlaylist(playlistId).executeAsOneOrNull()?.MAX ?: -1L).toInt()
    }

    suspend fun updatePlaylistSongCount(playlistId: String, count: Int) = withContext(Dispatchers.IO) {
        q.updatePlaylistSongCount(count.toLong(), playlistId)
    }
}

// ── Row mappers ───────────────────────────────────────────────────────────────

private fun Songs.toSong() = Song(
    id           = id,
    title        = title,
    artist       = artist,
    album        = album,
    duration     = duration,
    filePath     = filePath,
    url          = url,
    thumbnailUrl = thumbnailUrl,
    isLocal      = isLocal != 0L,
    isDownloaded = isDownloaded != 0L,
    dateAdded    = dateAdded,
    playCount    = playCount.toInt(),
    isLiked      = isLiked != 0L
)

private fun Playlists.toPlaylist() = Playlist(
    id           = id,
    name         = name,
    description  = description,
    thumbnailUrl = thumbnailUrl,
    dateCreated  = dateCreated,
    songCount    = songCount.toInt()
)

