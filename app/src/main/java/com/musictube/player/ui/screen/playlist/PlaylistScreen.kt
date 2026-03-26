package com.musictube.player.ui.screen.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musictube.player.data.model.Playlist
import com.musictube.player.data.model.Song
import com.musictube.player.ui.component.SongItem
import com.musictube.player.service.DownloadStatus
import com.musictube.player.viewmodel.PlaylistViewModel



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val allPlaylists by viewModel.allPlaylists.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val message by viewModel.message.collectAsState()

    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val isRepeatOn by viewModel.isRepeatOn.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlayingNow by viewModel.isPlaying.collectAsState()
    val miniPlayerPosition by viewModel.currentPosition.collectAsState()
    val miniPlayerDuration by viewModel.duration.collectAsState()
    val miniPlayerQueueSize by viewModel.playQueueSize.collectAsState()
    // A song from this playlist is the active one when its id matches currentSong
    val playingSongId = currentSong?.id
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    // Captured before the action sheet closes so it survives onDismissRequest nulling selectedSong
    var songForPlaylistAdd by remember { mutableStateOf<Song?>(null) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    var showSongActionsSheet by remember { mutableStateOf(false) }
    var navigatingBack by remember { mutableStateOf(false) }

    // Multi-select state (only used on Offline Downloads)
    val isOfflinePlaylist = playlist?.name == "Offline Downloads"
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(emptySet<String>()) }
    var showBulkAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showBulkCreatePlaylistDialog by remember { mutableStateOf(false) }
    var bulkNewPlaylistName by remember { mutableStateOf("") }
    var showBulkMenu by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(playlist?.name ?: "Playlist") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navigatingBack) { navigatingBack = true; onNavigateBack() }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isSelectMode && songs.isNotEmpty() && !isOfflinePlaylist) {
                        IconButton(onClick = {
                            val playlistName = playlist?.name ?: "Playlist"
                            // Build a .musictube JSON file so the share shows as a tappable
                            // file attachment in Messenger/WhatsApp — custom URI schemes like
                            // mymusic:// are plain unclickable text in most messaging apps.
                            val sb = StringBuilder()
                            sb.append("{\"version\":1,\"name\":")
                            sb.append(org.json.JSONObject.quote(playlistName))
                            sb.append(",\"songs\":[")
                            songs.forEachIndexed { i, song ->
                                if (i > 0) sb.append(",")
                                val videoId = song.id.removePrefix("yt_").removePrefix("dl_")
                                sb.append("{\"id\":")
                                sb.append(org.json.JSONObject.quote(videoId))
                                sb.append(",\"title\":")
                                sb.append(org.json.JSONObject.quote(song.title))
                                sb.append(",\"artist\":")
                                sb.append(org.json.JSONObject.quote(song.artist))
                                sb.append("}")
                            }
                            sb.append("]}")
                            val safeFileName = playlistName.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
                            val dir = File(context.cacheDir, "shared_playlists").also { it.mkdirs() }
                            val file = File(dir, "$safeFileName.musictube")
                            file.writeText(sb.toString())
                            val fileUri: Uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", file
                            )
                            val songLines = songs.take(5).joinToString("\n") { "  \u2022 ${it.title}" } +
                                if (songs.size > 5) "\n  \u2026 +${songs.size - 5} more" else ""
                            val shareText = "\uD83C\uDFB5 $playlistName (${songs.size} song(s))\n$songLines\n\n\uD83D\uDCF2 Tap the .musictube file below to open in MusicTube.\n\u2139\uFE0F Don't have MusicTube? Ask the sender to share the app first."
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/x-musictube"
                                putExtra(Intent.EXTRA_SUBJECT, playlistName)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share playlist"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share playlist")
                        }
                    }
                    if (isOfflinePlaylist && isSelectMode) {
                        Text(
                            "${selectedIds.size} selected",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Box {
                            IconButton(onClick = { showBulkMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options")
                            }
                            DropdownMenu(
                                expanded = showBulkMenu,
                                onDismissRequest = { showBulkMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist") },
                                    onClick = {
                                        showBulkMenu = false
                                        showBulkAddToPlaylistDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Remove from Offline Downloads") },
                                    onClick = {
                                        showBulkMenu = false
                                        viewModel.removeSelectedSongs(selectedIds)
                                        selectedIds = emptySet()
                                        isSelectMode = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            // ─── NOW-PLAYING MINI-PLAYER BAR ─────────────────────────────────
            val song = currentSong
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
                                onClick = { if (isPlayingNow) viewModel.pausePlayback() else viewModel.resume() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    if (isPlayingNow) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlayingNow) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
        when {
            playlist == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            songs.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No downloaded songs in this playlist yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (isPlayingNow && songs.any { it.id == playingSongId }) {
                                    viewModel.pausePlayback()
                                } else {
                                    viewModel.playPlaylistFromStart()
                                    onNavigateToPlayer()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isPlayingNow && songs.any { it.id == playingSongId })
                                    Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isPlayingNow && songs.any { it.id == playingSongId })
                                    "Pause" else "Play Playlist"
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleShuffle() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffleOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleRepeat() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeatOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            PlaylistSongRow(
                                song = song,
                                isPlaying = song.id == playingSongId && isPlayingNow,
                                downloadStatus = downloadStatus[song.id] ?: DownloadStatus.IDLE,
                                downloadProgress = downloadProgress[song.id] ?: 0,
                                isSelectMode = isSelectMode,
                                isSelected = selectedIds.contains(song.id),
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + song.id
                                    else selectedIds - song.id
                                    if (selectedIds.isEmpty()) isSelectMode = false
                                },
                                onLongPress = {
                                    if (isOfflinePlaylist) {
                                        isSelectMode = true
                                        selectedIds = selectedIds + song.id
                                    }
                                },
                                onPlay = {
                                    if (isSelectMode) {
                                        selectedIds = if (selectedIds.contains(song.id))
                                            selectedIds - song.id else selectedIds + song.id
                                        if (selectedIds.isEmpty()) isSelectMode = false
                                    } else {
                                        viewModel.playSong(song)
                                        onNavigateToPlayer()
                                    }
                                },
                                onLikeClick = {
                                    viewModel.setLiked(song, !song.isLiked)
                                },
                                onOpenMenu = {
                                    selectedSong = song
                                    showSongActionsSheet = true
                                },
                                onDownload = { viewModel.downloadSong(song) }
                            )
                        }
                    }

                }
            }
        }
    }

    // Bulk add to playlist dialog
    if (showBulkAddToPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showBulkAddToPlaylistDialog = false },
            title = { Text("Add ${selectedIds.size} song(s) to Playlist") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            showBulkAddToPlaylistDialog = false
                            showBulkCreatePlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null)
                            Text("+ New Playlist", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    allPlaylists
                        .filter { !it.name.equals("Offline Downloads", ignoreCase = true) }
                        .forEach { pl ->
                            TextButton(
                                onClick = {
                                    val ids = selectedIds
                                    viewModel.addSelectedSongsToPlaylist(ids, pl.id)
                                    selectedIds = emptySet()
                                    isSelectMode = false
                                    showBulkAddToPlaylistDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(text = pl.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "${pl.songCount} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBulkAddToPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Bulk create new playlist dialog
    if (showBulkCreatePlaylistDialog) {
        val pendingIds = selectedIds
        AlertDialog(
            onDismissRequest = {
                showBulkCreatePlaylistDialog = false
                bulkNewPlaylistName = ""
            },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = bulkNewPlaylistName,
                    onValueChange = { bulkNewPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createPlaylistAndAddSelected(pendingIds, bulkNewPlaylistName)
                        selectedIds = emptySet()
                        isSelectMode = false
                        showBulkCreatePlaylistDialog = false
                        bulkNewPlaylistName = ""
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBulkCreatePlaylistDialog = false
                    bulkNewPlaylistName = ""
                }) { Text("Cancel") }
            }
        )
    }

    // Song actions bottom sheet
    if (showSongActionsSheet && selectedSong != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSongActionsSheet = false
                selectedSong = null
            }
        ) {
            SongActionSheet(
                song = selectedSong!!,
                playlistName = playlist?.name.orEmpty(),
                onDismiss = {
                    showSongActionsSheet = false
                    selectedSong = null
                },
                onToggleLike = {
                    viewModel.setLiked(selectedSong!!, !selectedSong!!.isLiked)
                },
                onRemove = {
                    viewModel.removeSong(selectedSong!!.id)
                    showSongActionsSheet = false
                    selectedSong = null
                },
                onAddToPlaylist = {
                    // Capture song BEFORE closing the sheet so it survives onDismissRequest
                    songForPlaylistAdd = selectedSong
                    showSongActionsSheet = false
                    showAddToPlaylistDialog = true
                }
            )
        }
    }

    // Playlist picker — AlertDialog avoids all sheet animation/state race conditions
    if (showAddToPlaylistDialog && songForPlaylistAdd != null) {
        AlertDialog(
            onDismissRequest = {
                showAddToPlaylistDialog = false
                songForPlaylistAdd = null
            },
            title = { Text("Add to Playlist") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            showAddToPlaylistDialog = false
                            showCreatePlaylistDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text("New Playlist")
                        }
                    }
                    allPlaylists.forEach { pl ->
                        TextButton(
                            onClick = {
                                viewModel.addSongToPlaylist(songForPlaylistAdd!!, pl.id)
                                showAddToPlaylistDialog = false
                                songForPlaylistAdd = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = pl.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${pl.songCount} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showAddToPlaylistDialog = false
                    songForPlaylistAdd = null
                }) { Text("Cancel") }
            }
        )
    }

    if (showCreatePlaylistDialog && songForPlaylistAdd != null) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
                songForPlaylistAdd = null
            },
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
                Button(
                    onClick = {
                        viewModel.createPlaylistAndAdd(songForPlaylistAdd!!, newPlaylistName)
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                        songForPlaylistAdd = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                        songForPlaylistAdd = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PlaylistSongRow(
    song: Song,
    isPlaying: Boolean,
    downloadStatus: DownloadStatus,
    downloadProgress: Int,
    isSelectMode: Boolean = false,
    isSelected: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onLongPress: () -> Unit = {},
    onPlay: () -> Unit,
    onLikeClick: () -> Unit,
    onOpenMenu: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(40.dp)
            )
        }
        SongItem(
            song = song,
            isPlaying = isPlaying,
            onClick = onPlay,
            onLikeClick = onLikeClick,
            onLongPress = onLongPress,
            modifier = Modifier.weight(1f)
        )

        if (!isSelectMode) {
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Song options"
                )
            }

            if (!song.isDownloaded) {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(44.dp)
                ) {
                    if (downloadStatus == DownloadStatus.DOWNLOADING) {
                        CircularProgressIndicator(
                            progress = { (downloadProgress.coerceIn(0, 100)) / 100f },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download offline",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        } // end if (!isSelectMode)
    }
}

@Composable
private fun SongActionSheet(
    song: Song,
    playlistName: String,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onRemove: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(text = song.title, style = MaterialTheme.typography.titleLarge)
        Text(text = "Song \u2022 ${song.artist}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        SheetActionRow(
            icon = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (song.isLiked) "Unlike" else "Like",
            onClick = onToggleLike
        )
        SheetActionRow(
            icon = Icons.Default.RemoveCircleOutline,
            label = if (playlistName == "Offline Downloads") "Remove from Offline" else "Remove from playlist",
            onClick = onRemove
        )
        SheetActionRow(
            icon = Icons.Default.LibraryMusic,
            label = "Add to Playlist",
            onClick = onAddToPlaylist
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}
