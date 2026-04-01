package com.musictube.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L,
    val filePath: String? = null,
    val url: String? = null,
    val thumbnailUrl: String? = null,
    val isLocal: Boolean = true,
    val isDownloaded: Boolean = false,
    val dateAdded: Long = 0L,
    val playCount: Int = 0,
    val isLiked: Boolean = false
)

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val dateCreated: Long = 0L,
    val songCount: Int = 0
)

@Serializable
data class PlaylistSong(
    val playlistId: String,
    val songId: String,
    val position: Int = 0,
    val dateAdded: Long = 0L
)

@Serializable
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
