package com.musictube.player.ui.screen.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musictube.player.service.DownloadStatus
import com.musictube.player.ui.component.SearchResultItem
import com.musictube.player.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadErrors by viewModel.downloadErrors.collectAsState()
    val downloadedVideoIds by viewModel.downloadedVideoIds.collectAsState()
    val previewVideoId by viewModel.previewVideoId.collectAsState()
    val previewIsPlaying by viewModel.previewIsPlaying.collectAsState()
    val previewIsLoading by viewModel.previewIsLoading.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Lazy list state for infinite scroll
    val lazyListState = rememberLazyListState()

    // Trigger load-more whenever the user scrolls within 5 items of the bottom.
    LaunchedEffect(lazyListState) {
        snapshotFlow {
            val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = lazyListState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }.collect { nearBottom ->
            if (nearBottom) {
                viewModel.loadMoreResults()
            }
        }
    }

    // Re-trigger after a load finishes in case snapshotFlow didn't re-emit
    // (nearBottom stays true the whole time when results fill less than a screen).
    // Small delay lets the ViewModel finish updating canLoadMore before we read it.
    LaunchedEffect(isLoadingMore) {
        if (!isLoadingMore && searchResults.isNotEmpty()) {
            kotlinx.coroutines.delay(100)
            if (canLoadMore) {
                val lastVisible = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = lazyListState.layoutInfo.totalItemsCount
                if (total > 0 && lastVisible >= total - 5) {
                    viewModel.loadMoreResults()
                }
            }
        }
    }
    
    var navigatingBack by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Music") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!navigatingBack) {
                            navigatingBack = true
                            viewModel.clearSearch()
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search for songs...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearSearch() }
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.searchMusic()
                        keyboardController?.hide()
                    }
                ),
                singleLine = true
            )

            // Search results
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    searchResults.isEmpty() && searchQuery.isNotEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No results found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    searchResults.isNotEmpty() -> {
                        LazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 240.dp // Extra bottom padding to avoid keyboard
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults, key = { it.id }) { result ->
                                SearchResultItem(
                                    searchResult = result,
                                    downloadStatus = if (result.id in downloadedVideoIds) DownloadStatus.COMPLETED
                                                     else (downloadStatus[result.id] ?: DownloadStatus.IDLE),
                                    downloadProgress = downloadProgress[result.id] ?: 0,
                                    downloadError = downloadErrors[result.id],
                                    isPreviewPlaying = previewVideoId == result.id && previewIsPlaying,
                                    isPreviewLoading = previewVideoId == result.id && previewIsLoading,
                                    onPreviewPlay = {
                                        keyboardController?.hide()
                                        viewModel.togglePreview(result)
                                    },
                                    onDownload = { viewModel.downloadSong(result) },
                                    onPlay = {
                                        viewModel.playSearchResult(result)
                                        keyboardController?.hide()
                                        onNavigateToPlayer()
                                    }
                                )
                            }
                            
                            // Show loading indicator at the bottom when fetching more
                            if (isLoadingMore && searchResults.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Search for your favorite songs",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}