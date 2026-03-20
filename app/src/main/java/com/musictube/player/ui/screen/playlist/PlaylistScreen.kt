package com.musictube.player.ui.screen.playlist

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
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

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    var showSongMenu by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                    Button(
                        onClick = {
                            viewModel.playPlaylistFromStart()
                            onNavigateToPlayer()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Play Playlist")
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            PlaylistSongRow(
                                song = song,
                                downloadStatus = downloadStatus[song.id] ?: DownloadStatus.IDLE,
                                downloadProgress = downloadProgress[song.id] ?: 0,
                                onPlay = {
                                    viewModel.playSong(song)
                                    onNavigateToPlayer()
                                },
                                onLikeClick = {
                                    viewModel.setLiked(song, !song.isLiked)
                                },
                                onOpenMenu = {
                                    selectedSong = song
                                    showSongMenu = true
                                },
                                onDownload = { viewModel.downloadSong(song) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSongMenu && selectedSong != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showSongMenu = false
                selectedSong = null
            }
        ) {
            SongActionSheet(
                song = selectedSong!!,
                playlistName = playlist?.name.orEmpty(),
                onDismiss = {
                    showSongMenu = false
                    selectedSong = null
                },
                onToggleLike = {
                    viewModel.setLiked(selectedSong!!, !selectedSong!!.isLiked)
                },
                onRemove = {
                    viewModel.removeSong(selectedSong!!.id)
                    showSongMenu = false
                    selectedSong = null
                },
                onAddToPlaylist = {
                    showSongMenu = false
                    showAddToPlaylistSheet = true
                }
            )
        }
    }

    if (showAddToPlaylistSheet && selectedSong != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddToPlaylistSheet = false
                selectedSong = null
            }
        ) {
            AddToPlaylistSheet(
                playlists = allPlaylists,
                onDismiss = {
                    showAddToPlaylistSheet = false
                    selectedSong = null
                },
                onCreateNew = {
                    showAddToPlaylistSheet = false
                    showCreatePlaylistDialog = true
                },
                onSelectPlaylist = { targetPlaylistId ->
                    viewModel.addSongToPlaylist(selectedSong!!, targetPlaylistId)
                    showAddToPlaylistSheet = false
                    selectedSong = null
                }
            )
        }
    }

    if (showCreatePlaylistDialog && selectedSong != null) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
                selectedSong = null
            },
            title = { Text("New playlist") },
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
                        viewModel.createPlaylistAndAdd(selectedSong!!, newPlaylistName)
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                        selectedSong = null
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
                        selectedSong = null
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
    downloadStatus: DownloadStatus,
    downloadProgress: Int,
    onPlay: () -> Unit,
    onLikeClick: () -> Unit,
    onOpenMenu: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongItem(
            song = song,
            onClick = onPlay,
            onLikeClick = onLikeClick,
            modifier = Modifier.weight(1f)
        )

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
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = song.title, style = MaterialTheme.typography.titleLarge)
        Text(text = "Song • ${song.artist}", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
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
            label = "add to playlist",
            onClick = onAddToPlaylist
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onSelectPlaylist: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "add to playlist", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onCreateNew) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("New playlist")
            }
        }
        playlists.forEach { playlist ->
            TextButton(
                onClick = { onSelectPlaylist(playlist.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = playlist.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "${playlist.songCount} songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SheetActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text = label)
        }
    }
}
