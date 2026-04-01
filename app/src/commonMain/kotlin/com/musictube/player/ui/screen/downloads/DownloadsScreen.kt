package com.musictube.player.ui.screen.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.musictube.player.data.model.SearchResult
import com.musictube.player.service.DownloadStatus
import com.musictube.player.viewmodel.DownloadsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = koinViewModel()
) {
    val queue       by viewModel.downloadQueue.collectAsState()
    val statusMap   by viewModel.downloadStatus.collectAsState()
    val progressMap by viewModel.downloadProgress.collectAsState()
    val errorsMap   by viewModel.downloadErrors.collectAsState()

    val sortedEntries = queue.values.sortedWith(compareBy {
        when (statusMap[it.id]) {
            DownloadStatus.DOWNLOADING -> 0
            DownloadStatus.FAILED -> 1
            else -> 2
        }
    })

    var navigatingBack by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = { if (!navigatingBack) { navigatingBack = true; onNavigateBack() } }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (sortedEntries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No downloads yet", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedEntries, key = { it.id }) { result ->
                    DownloadItemRow(
                        result = result,
                        status = statusMap[result.id] ?: DownloadStatus.IDLE,
                        progress = progressMap[result.id] ?: 0,
                        error = errorsMap[result.id],
                        onRetry = { viewModel.retryDownload(result.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadItemRow(
    result: SearchResult,
    status: DownloadStatus,
    progress: Int,
    error: String?,
    onRetry: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().then(
            if (status == DownloadStatus.FAILED) Modifier.clickable(onClick = onRetry) else Modifier
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = result.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(result.title, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                when (status) {
                    DownloadStatus.DOWNLOADING -> {
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("$progress%", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DownloadStatus.FAILED -> {
                        Text(error ?: "Download failed", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Tap to retry", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    }
                    DownloadStatus.COMPLETED -> {
                        Text("Downloaded", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    else -> {}
                }
            }
            when (status) {
                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                DownloadStatus.COMPLETED -> Icon(Icons.Default.CheckCircle, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                DownloadStatus.FAILED -> Icon(Icons.Default.ErrorOutline, null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                else -> {}
            }
        }
    }
}
