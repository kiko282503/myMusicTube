package com.musictube.player

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.musictube.player.service.DownloadStatus
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.ui.screen.downloads.DownloadsScreen
import com.musictube.player.ui.screen.home.HomeScreen
import com.musictube.player.ui.screen.player.PlayerScreen
import com.musictube.player.ui.screen.playlist.PlaylistScreen
import com.musictube.player.ui.screen.quickpicks.QuickPicksScreen
import com.musictube.player.ui.screen.search.SearchScreen
import com.musictube.player.ui.theme.MusicTubeTheme
import com.musictube.player.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var playerManager: MusicPlayerManager

    // Drives navigation requests from notification intents into the Compose NavHost.
    private var pendingDestination by mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDestination = intent?.getStringExtra("navigate_to")
        
        setContent {
            MusicTubeTheme {
                MusicTubeApp(
                    pendingDestination = pendingDestination,
                    onPendingConsumed = { pendingDestination = null }
                )
            }
        }
    }

    /** Called when the Activity already exists and a new intent is delivered (e.g. notification tap). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = intent.getStringExtra("navigate_to")
    }
    
    override fun onResume() {
        super.onResume()
        // Resume WebView when app comes to foreground
        playerManager.resumeWebView()
    }
    
    override fun onPause() {
        super.onPause()
        // Park WebView in decor view so audio keeps playing in background
        playerManager.parkWebView(this)
    }
    
    override fun onStop() {
        super.onStop()
        // Even when stopped, try to keep WebView active
        playerManager.parkWebView(this)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicTubeApp(
    pendingDestination: String? = null,
    onPendingConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val downloadStatus by mainViewModel.downloadStatus.collectAsState()
    val activeDownloadCount = downloadStatus.values.count { it == DownloadStatus.DOWNLOADING }
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Navigate to a screen requested by an incoming intent (e.g. notification tap).
    LaunchedEffect(pendingDestination) {
        val dest = pendingDestination
        if (dest != null) {
            navController.navigate(dest) { launchSingleTop = true }
            onPendingConsumed()
        }
    }
    
    // Request storage permissions
    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    val permissionState = rememberPermissionState(storagePermission)

    // Request notification permission on Android 13+ (needed for foreground service notification)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null
    
    LaunchedEffect(permissionState.status) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
        notificationPermissionState?.let {
            if (!it.status.isGranted) it.launchPermissionRequest()
        }
    }

    Column(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        // Global download progress banner — hidden on the downloads screen itself
        if (activeDownloadCount > 0 && currentRoute != "downloads") {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("downloads") { launchSingleTop = true }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.weight(1f)
        ) {
        composable("home") {
            HomeScreen(
                onNavigateToPlayer = { navController.navigate("player") { launchSingleTop = true } },
                onNavigateToSearch = { navController.navigate("search") { launchSingleTop = true } },
                onNavigateToQuickPicks = { navController.navigate("quick_picks") { launchSingleTop = true } },
                onNavigateToPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") { launchSingleTop = true } }
            )
        }
        
        composable("player") {
            PlayerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("search") {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") { launchSingleTop = true } }
            )
        }

        composable("quick_picks") {
            QuickPicksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") { launchSingleTop = true } }
            )
        }

        composable("playlist/{playlistId}") {
            PlaylistScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") { launchSingleTop = true } }
            )
        }

        composable("downloads") {
            DownloadsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
    } // end Column
}

@Preview(showBackground = true)
@Composable
fun MusicTubeAppPreview() {
    MusicTubeTheme {
        MusicTubeApp()
    }
}