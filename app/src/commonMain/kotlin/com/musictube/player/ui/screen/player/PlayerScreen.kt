package com.musictube.player.ui.screen.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.musictube.player.data.model.Playlist
import com.musictube.player.service.DownloadStatus
import com.musictube.player.viewmodel.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = koinViewModel()
) {
    val currentSong     by viewModel.currentSong.collectAsState()
    val isPlaying       by viewModel.isPlaying.collectAsState()
    val isLoadingStream by viewModel.isLoadingStream.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration        by viewModel.duration.collectAsState()
    val downloadStatus  by viewModel.currentDownloadStatus.collectAsState()
    val downloadProgress by viewModel.currentDownloadProgress.collectAsState()
    val playlists       by viewModel.getPlaylistList().collectAsState(initial = emptyList())
    val isInAnyPlaylist by viewModel.isCurrentSongInAnyPlaylist.collectAsState()
    val hasPrevious     by viewModel.hasPrevious.collectAsState()
    val hasNext         by viewModel.hasNext.collectAsState()
    val isShuffleOn     by viewModel.isShuffleOn.collectAsState()
    val isRepeatOn      by viewModel.isRepeatOn.collectAsState()
    val playQueueSize   by viewModel.playQueueSize.collectAsState()

    var showPlaylistDialog      by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName         by remember { mutableStateOf("") }
    var navigatingBack          by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = { if (!navigatingBack) { navigatingBack = true; onNavigateBack() } }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        val song = currentSong
        if (song != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Thumbnail
                if (!song.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(240.dp).padding(horizontal = 32.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Spacer(Modifier.height(8.dp))

                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(song.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when {
                            isLoadingStream -> "Loading..."
                            isPlaying -> "Playing"
                            currentPosition == 0L -> "Stopped"
                            else -> "Paused"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isPlaying -> MaterialTheme.colorScheme.primary
                            currentPosition == 0L -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    if (isLoadingStream) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    } else if (duration > 0) {
                        Text("${formatTime(currentPosition)} / ${formatTime(duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth())
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Shuffle / Repeat (shown only for queue)
                if (playQueueSize > 1) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(Icons.Default.Shuffle, "Shuffle",
                                tint = if (isShuffleOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(Icons.Default.Repeat, "Repeat",
                                tint = if (isRepeatOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }

                // Playback controls
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) {
                    if (playQueueSize > 1) {
                        IconButton(onClick = { viewModel.playPrevious() }, Modifier.size(56.dp), enabled = hasPrevious) {
                            Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(32.dp),
                                tint = if (hasPrevious) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                    IconButton(onClick = { viewModel.stop() }, Modifier.size(56.dp)) {
                        Icon(Icons.Default.Stop, "Stop", Modifier.size(28.dp))
                    }
                    IconButton(onClick = { if (isPlaying) viewModel.pause() else viewModel.resume() },
                        Modifier.size(72.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play", Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    if (playQueueSize > 1) {
                        IconButton(onClick = { viewModel.playNext() }, Modifier.size(56.dp), enabled = hasNext) {
                            Icon(Icons.Default.SkipNext, "Next", Modifier.size(32.dp),
                                tint = if (hasNext) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Download + playlist buttons
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    when (downloadStatus) {
                        DownloadStatus.IDLE -> OutlinedButton(onClick = { viewModel.downloadCurrentSong() },
                            enabled = !isLoadingStream) {
                            Icon(Icons.Default.Download, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download")
                        }
                        DownloadStatus.DOWNLOADING -> OutlinedButton(onClick = {}, enabled = false) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("$downloadProgress%")
                        }
                        DownloadStatus.COMPLETED -> Button(onClick = {}) {
                            Icon(Icons.Default.Check, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Downloaded")
                        }
                        DownloadStatus.FAILED -> OutlinedButton(onClick = { viewModel.downloadCurrentSong() }) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                    OutlinedButton(onClick = { showPlaylistDialog = true }) {
                        Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isInAnyPlaylist) "In Playlist" else "Add to Playlist")
                    }
                }
            }
        } else {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))
                    Text("No song playing", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }

    // Add to playlist dialog
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                LazyColumn {
                    items(playlists) { playlist ->
                        TextButton(onClick = {
                            viewModel.addCurrentSongToPlaylist(playlist.id)
                            showPlaylistDialog = false
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(playlist.name)
                        }
                    }
                    item {
                        TextButton(onClick = { showPlaylistDialog = false; showCreatePlaylistDialog = true },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Create New Playlist")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel") } }
        )
    }
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPlaylistName.isNotBlank()) {
                        viewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                        showCreatePlaylistDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") } }
        )
    }
}
