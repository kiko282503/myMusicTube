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
import coil3.compose.SubcomposeAsyncImage
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.DownloadStatus
import com.musictube.player.ui.component.SearchResultItem
import com.musictube.player.viewmodel.HomeViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToQuickPicks: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val trendingSongs        by viewModel.trendingSongs.collectAsState()
    val quickPicks           by viewModel.quickPicks.collectAsState()
    val playlists            by viewModel.playlists.collectAsState()
    val offlinePlaylistId    by viewModel.offlinePlaylistId.collectAsState()
    val offlineSongCount     by viewModel.offlineSongCount.collectAsState()
    val isLoading            by viewModel.isLoading.collectAsState()
    val isLoadingMore        by viewModel.isLoadingMore.collectAsState()
    val downloadStatus       by viewModel.downloadStatus.collectAsState()
    val downloadProgress     by viewModel.downloadProgress.collectAsState()
    val downloadedVideoIds   by viewModel.downloadedVideoIds.collectAsState()
    val nowPlayingSong       by viewModel.currentSong.collectAsState()
    val isPlayingNow         by viewModel.isPlayingNow.collectAsState()
    val miniPlayerPosition   by viewModel.currentPosition.collectAsState()
    val miniPlayerDuration   by viewModel.duration.collectAsState()
    val miniPlayerQueueSize  by viewModel.playQueueSize.collectAsState()
    val previewVideoId       by viewModel.previewVideoId.collectAsState()
    val previewIsPlaying     by viewModel.previewIsPlaying.collectAsState()
    val previewIsLoading     by viewModel.previewIsLoading.collectAsState()
    val playbackError        by viewModel.playbackError.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playbackError) {
        val msg = playbackError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
    }

    var renamingPlaylist        by remember { mutableStateOf<Playlist?>(null) }
    var renameText              by remember { mutableStateOf("") }
    var deletingPlaylist        by remember { mutableStateOf<Playlist?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName         by remember { mutableStateOf("") }

    val favoritesName       = "Favorites"
    val offlineDownloadsName = "Offline Downloads"

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false; newPlaylistName = "" },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) viewModel.createPlaylist(newPlaylistName)
                    showCreatePlaylistDialog = false; newPlaylistName = ""
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false; newPlaylistName = "" }) { Text("Cancel") }
            }
        )
    }

    renamingPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { renamingPlaylist = null },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) viewModel.renamePlaylist(playlist.id, renameText)
                    renamingPlaylist = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { renamingPlaylist = null }) { Text("Cancel") } }
        )
    }

    deletingPlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { deletingPlaylist = null },
            title = { Text("Delete Playlist") },
            text = { Text("Delete \"${playlist.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deletePlaylist(playlist.id); deletingPlaylist = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deletingPlaylist = null }) { Text("Cancel") } }
        )
    }

    val homePlaylists by remember(playlists, offlinePlaylistId) {
        derivedStateOf {
            val offline   = playlists.firstOrNull { it.id == offlinePlaylistId }
            val favorites = playlists.firstOrNull { it.id != offlinePlaylistId && it.name.equals(favoritesName, ignoreCase = true) }
            val rest      = playlists.filter { it.id != offlinePlaylistId && !it.name.equals(favoritesName, ignoreCase = true) }
                .sortedByDescending { it.dateCreated }
            buildList {
                if (offline   != null) add(offline)
                if (favorites != null) add(favorites)
                addAll(rest)
            }
        }
    }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total       = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore && trendingSongs.isNotEmpty()) viewModel.loadMoreTrending()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            val song = nowPlayingSong
            if (song != null) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).clickable { onNavigateToPlayer() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                            Column {
                                Text(song.title, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Box(contentAlignment = Alignment.Center) {
                            if (miniPlayerDuration > 0) {
                                CircularProgressIndicator(
                                    progress = { (miniPlayerPosition.toFloat() / miniPlayerDuration.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.size(44.dp), strokeWidth = 2.5.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { if (isPlayingNow) viewModel.pause() else viewModel.resume() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    if (isPlayingNow) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlayingNow) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (miniPlayerQueueSize > 1) {
                            IconButton(onClick = { viewModel.playNext() }) {
                                Icon(Icons.Default.SkipNext, "Next",
                                    tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // ── Playlist section ──────────────────────────────────────────
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Playlist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showCreatePlaylistDialog = true }) {
                                Icon(Icons.Default.Add, "New Playlist", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("New Playlist")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (homePlaylists.isEmpty()) {
                                repeat(2) {
                                    Card(modifier = Modifier.width(112.dp), shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                        Column(Modifier.padding(8.dp)) {
                                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                                .clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                                Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {}
                                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                            }
                                            Spacer(Modifier.height(8.dp))
                                            Surface(Modifier.fillMaxWidth(0.8f).height(12.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {}
                                            Spacer(Modifier.height(4.dp))
                                            Surface(Modifier.fillMaxWidth(0.6f).height(10.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {}
                                        }
                                    }
                                }
                            } else {
                                homePlaylists.forEach { playlist ->
                                    val isSystem = playlist.id == offlinePlaylistId ||
                                        playlist.name.equals(favoritesName, ignoreCase = true)
                                    HomePlaylistCard(
                                        playlist = playlist,
                                        isOfflinePlaylist = playlist.id == offlinePlaylistId,
                                        offlineSongCount = offlineSongCount,
                                        isSystemPlaylist = isSystem,
                                        onClick = { onNavigateToPlaylist(playlist.id) },
                                        onRename = { renameText = playlist.name; renamingPlaylist = playlist },
                                        onDelete = { deletingPlaylist = playlist }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Quick picks section ───────────────────────────────────────
                if (isLoading || quickPicks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quick picks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (quickPicks.isNotEmpty()) {
                                TextButton(onClick = onNavigateToQuickPicks) {
                                    Text("More →", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLoading && quickPicks.isEmpty()) {
                                repeat(3) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        repeat(2) {
                                            Card(modifier = Modifier.weight(1f).height(72.dp),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                                                        contentAlignment = Alignment.Center) {
                                                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {}
                                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                                    }
                                                    Column(Modifier.weight(1f).padding(horizontal = 10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Surface(Modifier.fillMaxWidth(0.8f).height(12.dp),
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {}
                                                        Surface(Modifier.fillMaxWidth(0.5f).height(10.dp),
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {}
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                quickPicks.chunked(2).forEach { rowItems ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowItems.forEach { pick ->
                                            QuickPickCard(
                                                modifier = Modifier.weight(1f),
                                                title = pick.title,
                                                artist = pick.artist,
                                                thumbnailUrl = pick.thumbnailUrl,
                                                onClick = { viewModel.playSearchResult(pick); onNavigateToPlayer() }
                                            )
                                        }
                                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Trending now section ──────────────────────────────────────
                item {
                    Text("Trending now", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp))
                }

                if (isLoading && trendingSongs.isEmpty()) {
                    items(5) {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(56.dp), shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(Modifier.fillMaxWidth(0.7f).height(14.dp),
                                        shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
                                    Surface(Modifier.fillMaxWidth(0.4f).height(10.dp),
                                        shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
                                }
                            }
                        }
                    }
                } else if (trendingSongs.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Couldn't load songs", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
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
                            isPreviewPlaying = previewVideoId == result.id && previewIsPlaying,
                            isPreviewLoading = previewVideoId == result.id && previewIsLoading,
                            onPreviewPlay  = { viewModel.togglePreview(result) },
                            onDownload     = { viewModel.downloadSong(result) },
                            onPlay         = { viewModel.playSearchResult(result); onNavigateToPlayer() }
                        )
                    }
                }

                item {
                    if (isLoadingMore) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp))
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
    isSystemPlaylist: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.width(112.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(8.dp)) {
            if (!playlist.thumbnailUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOfflinePlaylist) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOfflinePlaylist) Icons.Default.Download else Icons.Default.LibraryMusic,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = if (isOfflinePlaylist) MaterialTheme.colorScheme.onPrimaryContainer
                                   else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(playlist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (isOfflinePlaylist) "$offlineSongCount downloaded songs" else "${playlist.songCount} songs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (!isSystemPlaylist) {
                Box(Modifier.fillMaxWidth()) {
                    IconButton(onClick = { menuExpanded = true },
                        modifier = Modifier.size(28.dp).align(Alignment.CenterEnd)) {
                        Icon(Icons.Default.MoreVert, "Playlist options", Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menuExpanded = false; onRename() })
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }
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
        modifier = modifier.height(72.dp).clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
                contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(
                    model = thumbnailUrl.ifEmpty { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    },
                    error = {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f))
                            }
                        }
                    }
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (artist.isNotEmpty()) {
                    Text(artist, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

