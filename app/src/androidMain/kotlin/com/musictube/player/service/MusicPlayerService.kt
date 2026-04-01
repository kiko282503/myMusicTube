package com.musictube.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.musictube.player.MainActivity
import com.musictube.player.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import org.koin.android.ext.android.inject

class MusicPlayerService : Service() {

    private val playerManager: MusicPlayerManager by inject()

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var thumbnailJob: Job? = null
    private var monitorJob: Job? = null
    private var cachedArt: Bitmap? = null
    private var cachedArtUrl: String? = null

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY    = "com.musictube.ACTION_PLAY"
        const val ACTION_PAUSE   = "com.musictube.ACTION_PAUSE"
        const val ACTION_STOP    = "com.musictube.ACTION_STOP"
        const val ACTION_PREV    = "com.musictube.ACTION_PREV"
        const val ACTION_NEXT    = "com.musictube.ACTION_NEXT"
        const val ACTION_DISMISS = "com.musictube.ACTION_DISMISS"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> if (playerManager.currentSong.value != null) playerManager.resume()
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                            if (playerManager.currentSong.value != null) playerManager.pause()
                    }
                }
                build()
            }
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> if (playerManager.currentSong.value != null) playerManager.resume()
                        AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->
                            if (playerManager.currentSong.value != null) playerManager.pause()
                    }
                },
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }

        mediaSession = MediaSessionCompat(this, "MusicTube").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay()  { playerManager.resume(); updateNotificationPlayState(true) }
                override fun onPause() { playerManager.pause(); updateNotificationPlayState(false) }
                override fun onStop()  { playerManager.stop(); stopSelf() }
                override fun onSeekTo(pos: Long) { playerManager.seekTo(pos) }
                override fun onSkipToPrevious() { playerManager.playPrevious() }
                override fun onSkipToNext()     { playerManager.playNext() }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY    -> { playerManager.resume(); updateNotificationPlayState(true) }
            ACTION_PAUSE   -> { playerManager.pause(); updateNotificationPlayState(false) }
            ACTION_PREV    -> playerManager.playPrevious()
            ACTION_NEXT    -> playerManager.playNext()
            ACTION_STOP    -> { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            ACTION_DISMISS -> { playerManager.pause(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
            else -> {
                val title = intent?.getStringExtra("title")
                val artist = intent?.getStringExtra("artist")
                val thumbnailUrl = intent?.getStringExtra("thumbnail")
                if (title != null && artist != null) {
                    showNotification(title, artist, playerManager.isPlaying.value, thumbnailUrl)
                } else {
                    val song = playerManager.currentSong.value
                    if (song != null) showNotification(song.title, song.artist, playerManager.isPlaying.value, song.thumbnailUrl)
                }
            }
        }
        return START_STICKY
    }

    fun showNotification(title: String, artist: String, isPlaying: Boolean, thumbnailUrl: String?) {
        updateMediaSession(title, artist, isPlaying, null)
        startForeground(NOTIFICATION_ID, buildNotification(title, artist, isPlaying, null))

        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                val song = playerManager.currentSong.value
                if (song != null) {
                    updateMediaSessionWithPosition(song.title, song.artist, playerManager.isPlaying.value,
                        playerManager.currentPosition.value, playerManager.duration.value, cachedArt)
                    if (playerManager.isPlaying.value && playerManager.currentVideoId.value != null)
                        playerManager.resumeWebView()
                }
            }
        }

        if (thumbnailUrl != null && thumbnailUrl != cachedArtUrl) {
            thumbnailJob?.cancel()
            thumbnailJob = scope.launch {
                val bmp = withContext(Dispatchers.IO) {
                    runCatching {
                        val conn = URL(thumbnailUrl).openConnection()
                        conn.connectTimeout = 5_000; conn.readTimeout = 5_000
                        BitmapFactory.decodeStream(conn.getInputStream())
                    }.getOrNull()
                }
                if (bmp != null) {
                    cachedArt = bmp; cachedArtUrl = thumbnailUrl
                    val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(title, artist, isPlaying, bmp))
                    updateMediaSession(title, artist, isPlaying, bmp)
                }
            }
        }
    }

    private fun updateMediaSession(title: String, artist: String, isPlaying: Boolean, art: Bitmap?) {
        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .apply { if (art != null) putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art) }
            .build()
        mediaSession.setMetadata(meta)
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            .build())
    }

    private fun updateMediaSessionWithPosition(title: String, artist: String, isPlaying: Boolean, position: Long, duration: Long, art: Bitmap?) {
        val meta = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .apply { if (art != null) putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art) }
            .build()
        mediaSession.setMetadata(meta)
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
            .setState(state, position, 1f)
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            .build())
    }

    private fun updateNotificationPlayState(isPlaying: Boolean) {
        val song = playerManager.currentSong.value ?: return
        updateMediaSession(song.title, song.artist, isPlaying, cachedArt)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(song.title, song.artist, isPlaying, cachedArt))
    }

    private fun buildNotification(title: String, artist: String, isPlaying: Boolean, art: Bitmap?): Notification {
        val openApp = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", "player")
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(title).setContentText(artist).setLargeIcon(art)
            .setContentIntent(openApp)
            .setDeleteIntent(pendingServiceIntent(ACTION_DISMISS, 3))
            .setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_skip_previous, "Previous", pendingServiceIntent(ACTION_PREV, 0))
            .addAction(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                       if (isPlaying) "Pause" else "Play",
                       pendingServiceIntent(if (isPlaying) ACTION_PAUSE else ACTION_PLAY, 1))
            .addAction(R.drawable.ic_skip_next, "Next", pendingServiceIntent(ACTION_NEXT, 2))
            .setStyle(MediaStyle().setMediaSession(mediaSession.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun pendingServiceIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(this, requestCode,
            Intent(this, MusicPlayerService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Music Playback", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows the current playing song with controls"
                setShowBadge(false)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        thumbnailJob?.cancel(); scope.cancel(); mediaSession.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        else @Suppress("DEPRECATION") audioManager.abandonAudioFocus(null)
        super.onDestroy()
    }
}
