package com.musictube.player.ui.screen.player

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musictube.player.data.model.Playlist
import com.musictube.player.service.DownloadStatus
import com.musictube.player.viewmodel.PlayerViewModel

// Helper function to format time
fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000) / 60
    return "%d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val currentVideoId by viewModel.currentVideoId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoadingStream by viewModel.isLoadingStream.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val downloadStatus by viewModel.currentDownloadStatus.collectAsState()
    val downloadProgress by viewModel.currentDownloadProgress.collectAsState()
    val playlists by viewModel.getPlaylistList().collectAsState(initial = emptyList())
    val isInAnyPlaylist by viewModel.isCurrentSongInAnyPlaylist.collectAsState()
    val hasPrevious by viewModel.hasPrevious.collectAsState()
    val hasNext by viewModel.hasNext.collectAsState()
    val isShuffleOn by viewModel.isShuffleOn.collectAsState()
    val isRepeatOn by viewModel.isRepeatOn.collectAsState()
    val playQueueSize by viewModel.playQueueSize.collectAsState()
    val context = LocalContext.current
    
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Ensure the WebView is created (and permanently attached to the Activity content view)
    // whenever a YouTube song is active. No need to host it in Compose — GONE visibility
    // keeps it alive with audio/JS running without any view hierarchy juggling.
    if (currentVideoId != null) viewModel.getOrCreateWebView(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Now Playing") },
                navigationIcon = {
                    IconButton(onClick = {
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up")
                    }
                }
            )
        }
    ) { paddingValues ->

        val song = currentSong
        
        if (song != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Song info section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show playback status and progress
                    Text(
                        text = when {
                            isLoadingStream -> "Loading..."
                            isPlaying -> "Playing"
                            currentPosition == 0L -> "Stopped"
                            else -> "Paused"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            isLoadingStream -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            isPlaying -> MaterialTheme.colorScheme.primary
                            currentPosition == 0L -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        }
                    )
                    
                    // Show progress info - always visible if song exists
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLoadingStream) {
                        Text(
                            text = "Fetching stream...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else if (duration > 0) {
                        Text(
                            text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isPlaying) {
                        // Show loading state while duration is being detected
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // When stopped, show current position with empty progress
                        Text(
                            text = "${formatTime(currentPosition)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Playback controls
                Spacer(modifier = Modifier.height(16.dp))

                // Shuffle & Repeat toggles — only shown when a queue is active
                if (playQueueSize > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffleOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = "Repeat",
                                tint = if (isRepeatOn) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button — only shown for multi-song queues
                    if (playQueueSize > 1) {
                        IconButton(
                            onClick = { viewModel.playPrevious() },
                            modifier = Modifier.size(56.dp),
                            enabled = hasPrevious
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = if (hasPrevious) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(56.dp))
                    }

                    // Stop button
                    IconButton(
                        onClick = { viewModel.stop() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Play/Pause button (larger)
                    IconButton(
                        onClick = { 
                            if (isPlaying) {
                                viewModel.pause()
                            } else {
                                // If stopped (position = 0), restart from beginning
                                if (currentPosition == 0L) {
                                    viewModel.play()
                                } else {
                                    viewModel.resume()
                                }
                            }
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else if (currentPosition == 0L) "Play" else "Resume",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Next button — only shown for multi-song queues
                    if (playQueueSize > 1) {
                        IconButton(
                            onClick = { viewModel.playNext() },
                            modifier = Modifier.size(56.dp),
                            enabled = hasNext
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = if (hasNext) MaterialTheme.colorScheme.onSurface
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(56.dp))
                    }
                }
                
                // Download and Add to Playlist buttons
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download button — hidden once the song is fully downloaded
                    if (downloadStatus != DownloadStatus.COMPLETED) {
                        Button(
                            onClick = { viewModel.downloadCurrentSong() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            enabled = downloadStatus == DownloadStatus.IDLE || downloadStatus == DownloadStatus.FAILED
                        ) {
                            when (downloadStatus) {
                                DownloadStatus.IDLE -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Download")
                                        Text("Download")
                                    }
                                }
                                DownloadStatus.DOWNLOADING -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text("$downloadProgress%")
                                    }
                                }
                                DownloadStatus.FAILED -> {
                                    Text("Download Failed")
                                }
                                else -> {}
                            }
                        }
                    }

                    // Add to Playlist button
                    Button(
                        onClick = { showPlaylistDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = !isInAnyPlaylist
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add to Playlist")
                            Text("Playlist")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val thumbUrl = currentSong?.thumbnailUrl
                        if (!thumbUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = currentSong?.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = "🎵", style = MaterialTheme.typography.displayLarge)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No song playing",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
    
    // Playlist selection dialog
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Select Playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Column {
                        Text("No playlists yet. Create one to add this song.")
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Playlist name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        playlists.forEach { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addCurrentSongToPlaylist(playlist.id)
                                        showPlaylistDialog = false
                                    }
                                    .padding(vertical = 14.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPlaylistDialog = false
                                    showCreatePlaylistDialog = true
                                }
                                .padding(vertical = 14.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "New Playlist",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { showPlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create new playlist dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
            },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    enabled = newPlaylistName.isNotBlank()
                ) { Text("Create & Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                    newPlaylistName = ""
                }) { Text("Cancel") }
            }
        )
    }
}

