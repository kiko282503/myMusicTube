package com.musictube.player.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.DownloadStatus

@Composable
fun SearchResultItem(
    searchResult: SearchResult,
    downloadStatus: DownloadStatus,
    downloadProgress: Int = 0,
    downloadError: String? = null,
    isPreviewPlaying: Boolean = false,
    isPreviewLoading: Boolean = false,
    onDownload: () -> Unit,
    onPreviewPlay: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onPlay() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = searchResult.thumbnailUrl.ifEmpty { null },
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    },
                    error = {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Default.PlayArrow, null, Modifier.padding(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(searchResult.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(searchResult.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (searchResult.duration.isNotEmpty()) {
                        Text(searchResult.duration, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                if (searchResult.isPlayable && downloadStatus == DownloadStatus.DOWNLOADING) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0, 100) / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${downloadProgress.coerceIn(0, 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.width(4.dp))
            // Preview play/pause
            if (searchResult.isPlayable) {
                when {
                    isPreviewLoading -> Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    isPreviewPlaying -> IconButton(onClick = onPreviewPlay, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Pause, "Pause preview", tint = MaterialTheme.colorScheme.primary)
                    }
                    else -> IconButton(onClick = onPreviewPlay, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, "Play preview", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.width(4.dp))
            // Download action
            if (searchResult.isPlayable) {
                when (downloadStatus) {
                    DownloadStatus.IDLE -> IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Download, "Download", tint = MaterialTheme.colorScheme.primary)
                    }
                    DownloadStatus.DOWNLOADING -> Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    DownloadStatus.COMPLETED -> Icon(Icons.Default.Check, "Downloaded",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    DownloadStatus.FAILED -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ErrorOutline, "Retry", tint = MaterialTheme.colorScheme.error)
                        }
                        if (downloadError?.contains("rate", ignoreCase = true) == true) {
                            Text("Rate limited", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
