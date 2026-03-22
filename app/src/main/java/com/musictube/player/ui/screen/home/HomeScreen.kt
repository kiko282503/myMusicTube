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
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                    item {
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
                                if (homePlaylists.isEmpty()) {
                                    // Skeleton: 2 placeholder playlist cards while DB loads
                                    repeat(2) {
                                        Card(
                                            modifier = Modifier.width(112.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(10.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.fillMaxSize(),
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                    ) {}
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .height(12.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                ) {}
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.6f)
                                                        .height(10.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                ) {}
                                            }
                                        }
                                    }
                                } else {
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

                    // Quick picks section — show skeleton while loading, real cards once ready
                    if (isLoading || quickPicks.isNotEmpty()) {
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
                                if (quickPicks.isNotEmpty()) {
                                    TextButton(onClick = onNavigateToQuickPicks) {
                                        Text("More →", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isLoading && quickPicks.isEmpty()) {
                                    // Skeleton: 3 rows of 2 placeholder cards
                                    repeat(3) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            repeat(2) {
                                                Card(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(72.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(72.dp)
                                                                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Surface(
                                                                modifier = Modifier.fillMaxSize(),
                                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                            ) {}
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(20.dp),
                                                                strokeWidth = 2.dp
                                                            )
                                                        }
                                                        Column(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(horizontal = 10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Surface(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.8f)
                                                                    .height(12.dp),
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                            ) {}
                                                            Surface(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.5f)
                                                                    .height(10.dp),
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                                            ) {}
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val rows = quickPicks.chunked(2)
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
                    }

                    item {
                        Text(
                            text = "Trending now",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    if (isLoading && trendingSongs.isEmpty()) {
                        items(5) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column(
                                        Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth(0.7f)
                                                .height(14.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {}
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth(0.4f)
                                                .height(10.dp),
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {}
                                    }
                                }
                            }
                        }
                    } else if (trendingSongs.isEmpty()) {
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
                        items(trendingSongs, key = { it.id }) { result ->
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
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(playlist.thumbnailUrl)
                        .crossfade(200)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp)),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
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
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl.ifEmpty { null })
                        .crossfade(200)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    },
                    error = {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                )
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

