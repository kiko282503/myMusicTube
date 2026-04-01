package com.musictube.player.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.musictube.player.ui.component.SearchResultItem
import com.musictube.player.viewmodel.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToPlayer: () -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val searchQuery       by viewModel.searchQuery.collectAsState()
    val searchResults     by viewModel.searchResults.collectAsState()
    val isLoading         by viewModel.isLoading.collectAsState()
    val errorMessage      by viewModel.errorMessage.collectAsState()
    val isLoadingMore     by viewModel.isLoadingMore.collectAsState()
    val downloadStatus    by viewModel.downloadStatus.collectAsState()
    val downloadProgress  by viewModel.downloadProgress.collectAsState()
    val previewVideoId    by viewModel.previewVideoId.collectAsState()
    val previewIsPlaying  by viewModel.previewIsPlaying.collectAsState()
    val previewIsLoading  by viewModel.previewIsLoading.collectAsState()

    val listState = rememberLazyListState()

    // Infinite scroll
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            val total = listState.layoutInfo.totalItemsCount
            lastVisible != null && lastVisible.index >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isLoadingMore && searchResults.isNotEmpty()) {
            viewModel.loadMoreResults()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Search") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                label = { Text("Search music…") },
                leadingIcon = {
                    if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Search, null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.searchMusic() })
            )

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall)
            }

            if (searchResults.isEmpty() && !isLoading && searchQuery.isNotBlank()) {
                Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.TopCenter) {
                    Text("No results found", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(searchResults, key = { it.id }) { result ->
                        SearchResultItem(
                            searchResult = result,
                            downloadStatus = downloadStatus[result.id] ?: com.musictube.player.service.DownloadStatus.IDLE,
                            downloadProgress = downloadProgress[result.id] ?: 0,
                            isPreviewPlaying = previewVideoId == result.id && previewIsPlaying,
                            isPreviewLoading = previewVideoId == result.id && previewIsLoading,
                            onPlay = {
                                viewModel.playSearchResult(result)
                                onNavigateToPlayer()
                            },
                            onPreviewPlay = { viewModel.togglePreview(result) },
                            onDownload = { viewModel.downloadSong(result) }
                        )
                    }
                    if (isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
