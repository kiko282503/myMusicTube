package com.musictube.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musictube.player.data.model.SharedPlaylistData
import com.musictube.player.service.DownloadStatus
import com.musictube.player.ui.screen.downloads.DownloadsScreen
import com.musictube.player.ui.screen.home.HomeScreen
import com.musictube.player.ui.screen.player.PlayerScreen
import com.musictube.player.ui.screen.playlist.PlaylistScreen
import com.musictube.player.ui.screen.quickpicks.QuickPicksScreen
import com.musictube.player.ui.screen.search.SearchScreen
import com.musictube.player.ui.screen.shared.SharedPlaylistScreen
import com.musictube.player.ui.theme.MusicTubeTheme
import com.musictube.player.viewmodel.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    pendingDestination: String? = null,
    onPendingConsumed: () -> Unit = {},
    pendingSharedPlaylist: SharedPlaylistData? = null,
    onSharedPlaylistConsumed: () -> Unit = {}
) {
    MusicTubeTheme {
        MusicTubeApp(
            pendingDestination = pendingDestination,
            onPendingConsumed = onPendingConsumed,
            pendingSharedPlaylist = pendingSharedPlaylist,
            onSharedPlaylistConsumed = onSharedPlaylistConsumed
        )
    }
}

@Composable
fun MusicTubeApp(
    pendingDestination: String? = null,
    onPendingConsumed: () -> Unit = {},
    pendingSharedPlaylist: SharedPlaylistData? = null,
    onSharedPlaylistConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = koinViewModel()
    val downloadStatus by mainViewModel.downloadStatus.collectAsState()
    val activeDownloadCount = downloadStatus.values.count { it == DownloadStatus.DOWNLOADING }
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    var sharedPlaylistData by remember { mutableStateOf<SharedPlaylistData?>(null) }

    // Handle incoming navigation intent (e.g. notification tap)
    LaunchedEffect(pendingDestination) {
        val dest = pendingDestination
        if (dest != null) {
            navController.navigate(dest) { launchSingleTop = true }
            onPendingConsumed()
        }
    }

    // Handle incoming shared playlist (e.g. deep link / file open)
    LaunchedEffect(pendingSharedPlaylist) {
        if (pendingSharedPlaylist != null) {
            sharedPlaylistData = pendingSharedPlaylist
            navController.navigate("shared_playlist") { launchSingleTop = true }
            onSharedPlaylistConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Global download progress banner — hidden on the downloads screen itself
        if (activeDownloadCount > 0 && currentRoute != "downloads") {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate("downloads") { launchSingleTop = true }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Icon(Icons.Default.CloudDownload, null, Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        text = if (activeDownloadCount == 1) "1 download in progress — tap to view"
                               else "$activeDownloadCount downloads in progress — tap to view",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        NavHost(navController = navController, startDestination = "home", modifier = Modifier.weight(1f)) {

            // When a shared playlist is active, the mini-player tap goes back to it instead of player
            val onNavigateToPlayer: () -> Unit = {
                if (sharedPlaylistData != null) {
                    navController.navigate("shared_playlist") { launchSingleTop = true }
                } else {
                    navController.navigate("player") { launchSingleTop = true }
                }
            }

            composable("home") {
                HomeScreen(
                    onNavigateToPlayer    = onNavigateToPlayer,
                    onNavigateToSearch    = { navController.navigate("search") { launchSingleTop = true } },
                    onNavigateToQuickPicks = { navController.navigate("quick_picks") { launchSingleTop = true } },
                    onNavigateToPlaylist  = { id -> navController.navigate("playlist/$id") { launchSingleTop = true } }
                )
            }

            composable("player") {
                PlayerScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable("search") {
                SearchScreen(onNavigateToPlayer = onNavigateToPlayer)
            }

            composable("quick_picks") {
                QuickPicksScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = onNavigateToPlayer
                )
            }

            composable("playlist/{playlistId}") {
                PlaylistScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = onNavigateToPlayer
                )
            }

            composable("downloads") {
                DownloadsScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable("shared_playlist") {
                SharedPlaylistScreen(
                    playlistData = sharedPlaylistData,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPlayer = { navController.navigate("player") { launchSingleTop = true } },
                    onDismiss = { sharedPlaylistData = null }
                )
            }
        }
    }
}
