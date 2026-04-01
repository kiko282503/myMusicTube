package com.musictube.player.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedSongData(
    val videoId: String,
    val title: String,
    val artist: String
)

@Serializable
data class SharedPlaylistData(
    val name: String,
    val songs: List<SharedSongData>
)
