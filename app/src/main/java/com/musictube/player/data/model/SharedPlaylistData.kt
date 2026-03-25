package com.musictube.player.data.model

data class SharedSongData(
    val videoId: String,  // raw YouTube ID (no yt_ prefix)
    val title: String,
    val artist: String
)

data class SharedPlaylistData(
    val name: String,
    val songs: List<SharedSongData>
)
