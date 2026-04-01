package com.musictube.player.ui.screen.playlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musictube.player.data.model.Song
import com.musictube.player.service.DownloadStatus
import com.musictube.player.ui.component.SongItem
import com.musictube.player.viewmodel.PlaylistViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: PlaylistViewModel = koinViewModel()
) {
    val playlist         by viewModel.playlist.collectAsState()
    val songs            by viewModel.songs.collectAsState()
    val allPlaylists     by viewModel.allPlaylists.collectAsState()
    val downloadStatus   by viewModel.downloadStatus.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val message          by viewModel.message.collectAsState()
    val isShuffleOn      by viewModel.isShuffleOn.collectAsState()
    val isRepeatOn       by viewModel.isRepeatOn.collectAsState()
    val currentSong      by viewModel.currentSong.collectAsState()
    val isPlaying     by viewModel.isPlaying.collectAsState()
    val miniPlayerPos    by viewModel.currentPosition.collectAsState()
    val miniPlayerDur    by viewModel.duration.collectAsState()
    val miniQueueSize    by viewModel.playQueueSize.collectAsState()
    val playingSongId = currentSong?.id

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSong          by remember { mutableStateOf<Song?>(null) }
    var songForPlaylistAdd    by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog  by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName       by remember { mutableStateOf("") }
    var showSongActionsSheet  by remember { mutableStateOf(false) }
    var navigatingBack        by remember { mutableStateOf(false) }

    val isOfflinePlaylist = playlist?.name == "Offline Downloads"
    var isSelectMode      by remember { mutableStateOf(false) }
    var selectedIds       by remember { mutableStateOf(emptySet<String>()) }
    var showBulkAddDialog    by remember { mutableStateOf(false) }
    var showBulkCreateDialog by remember { mutableStateOf(false) }
    var bulkNewPlaylistName  by remember { mutableStateOf("") }
    var showBulkMenu         by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = { if (!navigatingBack) { navigatingBack = true; onNavigateBack() } }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isOfflinePlaylist && isSelectMode) {
                        Text("${selectedIds.size} selected", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 4.dp))
                        Box {
                            IconButton(onClick = { showBulkMenu = true }) {
                                Icon(Icons.Default.MoreVert, "Options")
                            }
                            DropdownMenu(expanded = showBulkMenu, onDismissRequest = { showBulkMenu = false }) {
                                DropdownMenuItem(text = { Text("Add to Playlist") }, onClick = {
                                    showBulkMenu = false; showBulkAddDialog = true })
                                DropdownMenuItem(text = { Text("Remove from Offline Downloads") }, onClick = {
                                    showBulkMenu = false
                                    viewModel.removeSelectedSongs(selectedIds)
                                    selectedIds = emptySet(); isSelectMode = false
                                })
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            val song = currentSong
            if (song != null) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.MusicNote, null, Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(song.title, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Box(contentAlignment = Alignment.Center) {
                            if (miniPlayerDur > 0) {
                                CircularProgressIndicator(
                                    progress = { (miniPlayerPos.toFloat() / miniPlayerDur.toFloat()).coerceIn(0f, 1f) },
                                    modifier = Modifier.size(44.dp), strokeWidth = 2.5.dp,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            }
                            IconButton(onClick = { if (isPlaying) viewModel.pause() else viewModel.resume() },
                                modifier = Modifier.size(44.dp)) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (miniQueueSize > 1) {
                            IconButton(onClick = { viewModel.playNext() }) {
                                Icon(Icons.Default.SkipNext, "Next")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when {
            playlist == null -> Box(Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            songs.isEmpty() -> Box(Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center) {
                Text("No songs in this playlist yet", style = MaterialTheme.typography.bodyLarge) }
            else -> Column(Modifier.fillMaxSize().padding(paddingValues)) {
                // Play / shuffle / repeat controls
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        if (isPlaying && songs.any { it.id == playingSongId }) viewModel.pause()
                        else { viewModel.playPlaylistFromStart(); onNavigateToPlayer() }
                    }, modifier = Modifier.weight(1f)) {
                        Icon(if (isPlaying && songs.any { it.id == playingSongId })
                            Icons.Default.Pause else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying && songs.any { it.id == playingSongId })
                            "Pause" else "Play Playlist")
                    }
                    IconButton(onClick = { viewModel.toggleShuffle() }, Modifier.size(48.dp)) {
                        Icon(Icons.Default.Shuffle, "Shuffle", Modifier.size(26.dp),
                            tint = if (isShuffleOn) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    IconButton(onClick = { viewModel.toggleRepeat() }, Modifier.size(48.dp)) {
                        Icon(Icons.Default.Repeat, "Repeat", Modifier.size(26.dp),
                            tint = if (isRepeatOn) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
                LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(songs, key = { it.id }) { song ->
                        val isThisSelected = selectedIds.contains(song.id)
                        Row(Modifier.fillMaxWidth().combinedClickable(
                            onClick = {
                                if (isSelectMode) {
                                    selectedIds = if (isThisSelected) selectedIds - song.id else selectedIds + song.id
                                    if (selectedIds.isEmpty()) isSelectMode = false
                                } else { viewModel.playSong(song); onNavigateToPlayer() }
                            },
                            onLongClick = {
                                if (isOfflinePlaylist) { isSelectMode = true; selectedIds = selectedIds + song.id }
                            }
                        ), verticalAlignment = Alignment.CenterVertically) {
                            if (isSelectMode) {
                                Checkbox(checked = isThisSelected,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + song.id else selectedIds - song.id
                                        if (selectedIds.isEmpty()) isSelectMode = false
                                    })
                            }
                            SongItem(
                                song = song,
                                onClick = { viewModel.playSong(song); onNavigateToPlayer() },
                                onLikeClick = { viewModel.setLiked(song, !song.isLiked) },
                                onLongPress = {
                                    isSelectMode = true
                                    selectedIds = selectedIds + song.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Song actions bottom sheet
    if (showSongActionsSheet && selectedSong != null) {
        val sng = selectedSong!!
        ModalBottomSheet(onDismissRequest = { showSongActionsSheet = false }) {
            Column(Modifier.padding(16.dp)) {
                SheetActionRow(Icons.Default.Favorite, if (sng.isLiked) "Unlike" else "Like") {
                    viewModel.setLiked(sng, !sng.isLiked); showSongActionsSheet = false }
                SheetActionRow(Icons.Default.PlaylistAdd, "Add to Playlist") {
                    songForPlaylistAdd = sng; showSongActionsSheet = false; showAddToPlaylistDialog = true }
                SheetActionRow(Icons.Default.RemoveCircleOutline, "Remove from Playlist") {
                    viewModel.removeSong(sng); showSongActionsSheet = false }
            }
        }
    }

    // Add to playlist dialog
    if (showAddToPlaylistDialog && songForPlaylistAdd != null) {
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showAddToPlaylistDialog = false; showCreatePlaylistDialog = true },
                        Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("+ New Playlist") }
                    allPlaylists.filter { !it.name.equals("Offline Downloads", ignoreCase = true) }.forEach { pl ->
                        TextButton(onClick = {
                            viewModel.addSongToPlaylist(pl.id, songForPlaylistAdd!!)
                            showAddToPlaylistDialog = false
                        }, Modifier.fillMaxWidth()) { Text(pl.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddToPlaylistDialog = false }) { Text("Cancel") } }
        )
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it },
                label = { Text("Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylistAndAdd(newPlaylistName, songForPlaylistAdd!!)
                        newPlaylistName = ""; showCreatePlaylistDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") } }
        )
    }

    // Bulk add/create dialogs
    if (showBulkAddDialog) {
        AlertDialog(
            onDismissRequest = { showBulkAddDialog = false },
            title = { Text("Add ${selectedIds.size} song(s) to Playlist") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    TextButton(onClick = { showBulkAddDialog = false; showBulkCreateDialog = true },
                        Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("+ New Playlist") }
                    allPlaylists.filter { !it.name.equals("Offline Downloads", ignoreCase = true) }.forEach { pl ->
                        TextButton(onClick = {
                            viewModel.addSelectedSongsToPlaylist(selectedIds, pl.id)
                            selectedIds = emptySet(); isSelectMode = false; showBulkAddDialog = false
                        }, Modifier.fillMaxWidth()) { Text(pl.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showBulkAddDialog = false }) { Text("Cancel") } }
        )
    }
    if (showBulkCreateDialog) {
        val pendingIds = selectedIds
        AlertDialog(
            onDismissRequest = { showBulkCreateDialog = false; bulkNewPlaylistName = "" },
            title = { Text("New Playlist") },
            text = { OutlinedTextField(value = bulkNewPlaylistName, onValueChange = { bulkNewPlaylistName = it },
                label = { Text("Playlist name") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    viewModel.createPlaylistAndAddSelected(pendingIds, bulkNewPlaylistName)
                    selectedIds = emptySet(); isSelectMode = false; showBulkCreateDialog = false; bulkNewPlaylistName = ""
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showBulkCreateDialog = false; bulkNewPlaylistName = "" }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SheetActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
