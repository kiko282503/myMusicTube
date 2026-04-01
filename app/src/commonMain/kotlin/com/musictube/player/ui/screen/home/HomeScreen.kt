package com.musictube.player.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.musictube.player.data.model.SearchResult
import com.musictube.player.data.model.Song
import com.musictube.player.ui.component.SongItem
import com.musictube.player.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val recentSongs    by viewModel.recentSongs.collectAsState()
    val likedSongs     by viewModel.likedSongs.collectAsState()
    val playlists      by viewModel.playlists.collectAsState()
    val currentSong    by viewModel.currentSong.collectAsState()
    val isPlaying      by viewModel.isPlaying.collectAsState()
    val homeFeeds      by viewModel.homeFeeds.collectAsState()
    val isFeedLoading  by viewModel.isFeedLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MusicTube") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Now playing mini bar
            if (currentSong != null) {
                item {
                    NowPlayingBar(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        onTap = onNavigateToPlayer,
                        onPlayPause = { if (isPlaying) viewModel.pause() else viewModel.resume() }
                    )
                }
            }

            // Home feed rows
            if (isFeedLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                homeFeeds.forEach { (title, songs) ->
                    item {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                    }
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(songs, key = { it.id }) { song ->
                                FeedSongCard(searchResult = song, onClick = {
                                    viewModel.togglePreview(song)
                                    onNavigateToPlayer()
                                })
                            }
                        }
                    }
                }
            }

            // Recent songs section
            if (recentSongs.isNotEmpty()) {
                item {
                    Text("Recently Played", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }
                items(recentSongs.take(10), key = { "recent_${it.id}" }) { song ->
                    SongItem(
                        song = song,
                        onClick = { viewModel.playSong(song); onNavigateToPlayer() },
                        onLikeClick = { viewModel.toggleLike(song) }
                    )
                }
            }

            // Liked songs section
            if (likedSongs.isNotEmpty()) {
                item {
                    Text("Liked Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
                }
                items(likedSongs.take(10), key = { "liked_${it.id}" }) { song ->
                    SongItem(
                        song = song,
                        onClick = { viewModel.playSong(song); onNavigateToPlayer() },
                        onLikeClick = { viewModel.toggleLike(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingBar(
    song: com.musictube.player.data.model.Song,
    isPlaying: Boolean,
    onTap: () -> Unit,
    onPlayPause: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!song.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(model = song.thumbnailUrl, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play")
            }
        }
    }
}

@Composable
private fun FeedSongCard(
    searchResult: com.musictube.player.data.model.SearchResult,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.width(160.dp).clickable(onClick = onClick)) {
        Column {
            if (searchResult.thumbnailUrl.isNotBlank()) {
                AsyncImage(model = searchResult.thumbnailUrl, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(100.dp))
            } else {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(searchResult.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(searchResult.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
