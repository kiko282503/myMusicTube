package com.musictube.player.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Entity(tableName = "songs")
@Parcelize
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L, // Duration in milliseconds
    val filePath: String? = null, // Local file path
    val url: String? = null, // Online URL if downloaded from web
    val thumbnailUrl: String? = null,
    val isLocal: Boolean = true,
    val isDownloaded: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val playCount: Int = 0,
    val isLiked: Boolean = false
) : Parcelable

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val songCount: Int = 0
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"]
)
data class PlaylistSong(
    val playlistId: String,
    val songId: String,
    val position: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)

data class SearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val duration: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val audioUrl: String? = null,
    val itemType: String = "song",
    val isPlayable: Boolean = true
)