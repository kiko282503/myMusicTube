package com.musictube.player.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.musictube.player.MainActivity

/**
 * Foreground service that keeps the app process alive and holds a CPU WakeLock
 * while audio downloads are in progress.
 *
 * Without this, Android's Doze mode suspends background network I/O as soon as
 * the screen is locked, causing downloads to stall or time out.
 *
 * Started by DownloadManager when the first download begins,
 * stopped when the last active download completes or fails.
 */
class DownloadForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "com.musictube.DOWNLOAD_START"
        const val ACTION_STOP  = "com.musictube.DOWNLOAD_STOP"
        const val CHANNEL_ID   = "download_channel"
        const val NOTIFICATION_ID = 2
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification())
                acquireWakeLock()
            }
            ACTION_STOP -> {
                releaseWakeLock()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MusicTube:DownloadWakeLock"
        ).apply {
            acquire(30 * 60 * 1000L) // 30-minute safety timeout
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active download progress"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Downloading songs…")
        .setContentText("Downloads will continue while screen is off")
        .setOngoing(true)
        .setSilent(true)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()
}
