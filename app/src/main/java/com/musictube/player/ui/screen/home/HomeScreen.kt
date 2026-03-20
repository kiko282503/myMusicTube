package com.musictube.player.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musictube.player.data.model.Playlist
import com.musictube.player.ui.component.SearchResultItem
import com.musictube.player.service.DownloadStatus
import com.musictube.player.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQuickPicks: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val trendingSongs by viewModel.trendingSongs.collectAsState()
    val quickPicks by viewModel.quickPicks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val offlinePlaylistId by viewModel.offlinePlaylistId.collectAsState()
    val offlineSongCount by viewModel.offlineSongCount.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadedVideoIds by viewModel.downloadedVideoIds.collectAsState()
    val nowPlayingSong by viewModel.currentSong.collectAsState()
    val isPlayingNow by viewModel.isPlayingNow.collectAsState()
    val miniPlayerPosition by viewModel.currentPosition.collectAsState()
    val miniPlayerDuration by viewModel.duration.collectAsState()
    val miniPlayerQueueSize by viewModel.playQueueSize.collectAsState()

    val homePlaylists by remember(playlists, offlinePlaylistId) {
        derivedStateOf {
            val offline = playlists.firstOrNull { it.id == offlinePlaylistId }
            val userPlaylists = playlists
                .asSequence()
                .filter { it.id != offlinePlaylistId }
                .sortedByDescending { it.dateCreated }
                .toList()

            buildList {
                if (offline != null) add(offline)
                addAll(userPlaylists)
            }
        }
    }

    val listState = rememberLazyListState()

    // Trigger load-more when user scrolls near the last 3 items
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore && trendingSongs.isNotEmpty()) {
            viewModel.loadMoreTrending()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MusicTube", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToSearch,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search Music")
            }
        },
        bottomBar = {
            // Show Now Playing bar when a song is active so the user can return to the player
            val song = nowPlayingSong
            if (song != null) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Tapping the song info area navigates to the full player
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToPlayer() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        // Play/pause button with circular progress ring
                        Box(contentAlignment = Alignment.Center) {
                            if (miniPlayerDuration > 0) {
                                CircularProgressIndicator(
                                    progress = {
                                        (miniPlayerPosition.toFloat() / miniPlayerDuration.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.size(44.dp),
                                    strokeWidth = 2.5.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { if (isPlayingNow) viewModel.pause() else viewModel.resume() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    if (isPlayingNow) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingNow) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Skip next — only when playing from a multi-song queue
                        if (miniPlayerQueueSize > 1) {
                            IconButton(onClick = { viewModel.playNext() }) {
                                Icon(
                                    Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && trendingSongs.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        if (homePlaylists.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Text(
                                    text = "Playlist",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    homePlaylists.forEach { playlist ->
                                        HomePlaylistCard(
                                            playlist = playlist,
                                            isOfflinePlaylist = playlist.id == offlinePlaylistId,
                                            offlineSongCount = offlineSongCount,
                                            onClick = { onNavigateToPlaylist(playlist.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (quickPicks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Quick picks",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onNavigateToQuickPicks) {
                                    Text("More →", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                        item {
                            val rows = quickPicks.chunked(2)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rows.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { pick ->
                                            QuickPickCard(
                                                modifier = Modifier.weight(1f),
                                                title = pick.title,
                                                artist = pick.artist,
                                                thumbnailUrl = pick.thumbnailUrl,
                                                onClick = {
                                                    viewModel.playSearchResult(pick)
                                                    onNavigateToPlayer()
                                                }
                                            )
                                        }
                                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Trending now",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    if (trendingSongs.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Couldn't load songs",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.reloadTrending() }) { Text("Retry") }
                            }
                        }
                    } else {
                        items(trendingSongs) { result ->
                            SearchResultItem(
                                searchResult = result,
                                downloadStatus = if (result.id in downloadedVideoIds) DownloadStatus.COMPLETED
                                                 else (downloadStatus[result.id] ?: DownloadStatus.IDLE),
                                downloadProgress = downloadProgress[result.id] ?: 0,
                                onDownload = { viewModel.downloadSong(result) },
                                onPlay = {
                                    viewModel.playSearchResult(result)
                                    onNavigateToPlayer()
                                }
                            )
                        }
                    }

                    item {
                        if (isLoadingMore) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePlaylistCard(
    playlist: Playlist,
    isOfflinePlaylist: Boolean,
    offlineSongCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(112.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (!playlist.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOfflinePlaylist) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOfflinePlaylist) {
                                Icons.Default.Download
                            } else {
                                Icons.Default.LibraryMusic
                            },
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = if (isOfflinePlaylist) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = if (isOfflinePlaylist) {
                    "$offlineSongCount downloaded songs"
                } else {
                    "${playlist.songCount} songs"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickPickCard(
    title: String,
    artist: String,
    thumbnailUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square thumbnail on the left
            if (thumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (artist.isNotEmpty()) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

