package com.musictube.player

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.musictube.player.data.model.SharedPlaylistData
import com.musictube.player.data.model.SharedSongData
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.ui.theme.MusicTubeTheme
import org.json.JSONObject
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val playerManager: MusicPlayerManager by inject()

    private var pendingDestination: String? = null
    private var pendingSharedPlaylist: SharedPlaylistData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDestination = intent?.getStringExtra("navigate_to")
        pendingSharedPlaylist = intent?.let { resolveSharedPlaylist(it) }

        setContent {
            MusicTubeTheme {
                App(
                    pendingDestination = pendingDestination,
                    onPendingConsumed = { pendingDestination = null },
                    pendingSharedPlaylist = pendingSharedPlaylist,
                    onSharedPlaylistConsumed = { pendingSharedPlaylist = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = intent.getStringExtra("navigate_to")
        pendingSharedPlaylist = resolveSharedPlaylist(intent)
    }

    private fun resolveSharedPlaylist(intent: Intent): SharedPlaylistData? {
        val action = intent.action ?: ""
        val fileUri: Uri? = when (action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (fileUri != null && fileUri.scheme != "mymusic") {
            val parsed = parseMusictubefile(fileUri)
            if (parsed != null) return parsed
        }
        return intent.data?.let { parseSharedPlaylistUri(it) }
    }

    private fun parseMusictubefile(uri: Uri): SharedPlaylistData? {
        return try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return null
            val root = JSONObject(text)
            val name = root.optString("name").takeIf { it.isNotBlank() } ?: return null
            val arr = root.optJSONArray("songs") ?: return null
            val songs = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val title = obj.optString("title").ifBlank { "Unknown" }
                val artist = obj.optString("artist").ifBlank { "Unknown" }
                SharedSongData(videoId = id, title = title, artist = artist)
            }
            if (songs.isEmpty()) null else SharedPlaylistData(name = name, songs = songs)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSharedPlaylistUri(uri: Uri): SharedPlaylistData? {
        val isCustomScheme = uri.scheme == "mymusic" && uri.host == "playlist"
        val isHttpsScheme = uri.scheme == "https" && uri.host == "open.mymusic.app" &&
            uri.pathSegments.firstOrNull() == "playlist"
        if (!isCustomScheme && !isHttpsScheme) return null
        val name = uri.getQueryParameter("name")?.takeIf { it.isNotBlank() } ?: return null
        val songsRaw = uri.getQueryParameter("songs_b64")
            ?.takeIf { it.isNotBlank() }
            ?.let { encoded ->
                runCatching {
                    android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE)
                        .toString(Charsets.UTF_8)
                }.getOrNull()
            }
            ?: uri.getQueryParameter("songs")?.takeIf { it.isNotBlank() }
            ?: return null
        val songs = songsRaw.split("|").mapNotNull { entry ->
            val parts = entry.split("::")
            if (parts.size >= 3) SharedSongData(videoId = parts[0], title = parts[1], artist = parts[2]) else null
        }
        if (songs.isEmpty()) return null
        return SharedPlaylistData(name = name, songs = songs)
    }

    override fun onResume() {
        super.onResume()
        playerManager.resumeWebView()
    }

    override fun onPause() {
        super.onPause()
        playerManager.parkWebView(this)
    }

    override fun onStop() {
        super.onStop()
        playerManager.parkWebView(this)
    }
}
