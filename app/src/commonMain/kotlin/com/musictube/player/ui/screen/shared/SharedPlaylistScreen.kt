package com.musictube.player.ui.screen.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musictube.player.data.model.SharedPlaylistData
import com.musictube.player.viewmodel.SharedPlaylistViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPlaylistScreen(
    playlistData: SharedPlaylistData?,
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit,
    onDismiss: () -> Unit = {},
    viewModel: SharedPlaylistViewModel = koinViewModel()
) {
    val message         by viewModel.message.collectAsState()
    val isSaved         by viewModel.isSaved.collectAsState()
    val currentSong     by viewModel.currentSong.collectAsState()
    val isPlaying       by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration        by viewModel.duration.collectAsState()
    val playingFromHere by viewModel.isPlayingFromHere.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNameText   by remember { mutableStateOf(playlistData?.name ?: "") }
    var navigatingBack by remember { mutableStateOf(false) }

    LaunchedEffect(playlistData) {
        playlistData?.let { viewModel.setPlaylistSongs(it) }
    }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }
    LaunchedEffect(isSaved) {
        if (isSaved) onDismiss()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Shared Playlist") },
                navigationIcon = {
                    IconButton(onClick = { if (!navigatingBack) { navigatingBack = true; onNavigateBack() } }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { onDismiss(); if (!navigatingBack) { navigatingBack = true; onNavigateBack() } }) {
                        Text(if (isSaved) "Close" else "Dismiss")
                    }
                }
            )
        },
        bottomBar = {
            if (currentSong != null && playingFromHere) {
                Surface(modifier = Modifier.fillMaxWidth().clickable { onNavigateToPlayer() },
                    color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 8.dp) {
                    Column {
                        if (duration > 0) {
                            LinearProgressIndicator(
                                progress = { (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(2.dp))
                        }
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.MusicNote, null, Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(currentSong!!.title, style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(currentSong!!.artist, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { if (isPlaying) viewModel.pause() else viewModel.resume() }) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "Pause" else "Resume",
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (playlistData == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No shared playlist data found.")
            }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(playlistData.name, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${playlistData.songs.size} song(s)", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    when {
                        playingFromHere && isPlaying -> viewModel.pause()
                        playingFromHere && !isPlaying -> viewModel.resume()
                        else -> { viewModel.playNow(); onNavigateToPlayer() }
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(if (playingFromHere && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(4.dp))
                    Text(when { playingFromHere && isPlaying -> "Pause"; playingFromHere -> "Resume"; else -> "Play Now" })
                }
                OutlinedButton(onClick = { saveNameText = playlistData.name; showSaveDialog = true },
                    enabled = !isSaved, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp))
                    Text(if (isSaved) "Saved ✓" else "Save Playlist")
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(playlistData.songs, key = { _, s -> s.videoId }) { index, song ->
                    val isThisPlaying = currentSong?.id == "yt_${song.videoId}"
                    Row(Modifier.fillMaxWidth()
                        .then(if (isThisPlaying) Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            MaterialTheme.shapes.small) else Modifier)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable { viewModel.playSongAt(index); onNavigateToPlayer() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isThisPlaying) {
                            Icon(Icons.Default.PlayArrow, "Now playing", Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Text("${index + 1}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.width(20.dp))
                        }
                        Icon(Icons.Default.MusicNote, null, Modifier.size(22.dp),
                            tint = if (isThisPlaying) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isThisPlaying) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Playlist") },
            text = {
                OutlinedTextField(value = saveNameText, onValueChange = { saveNameText = it },
                    label = { Text("Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveAsPlaylist(saveNameText)
                    showSaveDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }
}
