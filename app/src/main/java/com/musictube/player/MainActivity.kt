package com.musictube.player

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.ui.screen.home.HomeScreen
import com.musictube.player.ui.screen.player.PlayerScreen
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MusicTubeTheme {
                MusicTubeApp()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Resume WebView when app comes to foreground
        playerManager.resumeWebView()
    }
    
    override fun onPause() {
        super.onPause()
        // Park WebView to keep it active when app goes to background
        playerManager.parkWebView(this)
        
        // Additional measure: try to prevent YouTube from detecting the pause
        playerManager.resumeWebView()
    }
    
    override fun onStop() {
        super.onStop()
        // Even when stopped, try to keep WebView active
        playerManager.parkWebView(this)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicTubeApp() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    
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
    
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
        notificationPermissionState?.let {
            if (!it.status.isGranted) it.launchPermissionRequest()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToPlayer = { navController.navigate("player") },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToQuickPicks = { navController.navigate("quick_picks") }
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
                onNavigateToPlayer = { navController.navigate("player") }
            )
        }

        composable("quick_picks") {
            QuickPicksScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("player") }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MusicTubeAppPreview() {
    MusicTubeTheme {
        MusicTubeApp()
    }
}