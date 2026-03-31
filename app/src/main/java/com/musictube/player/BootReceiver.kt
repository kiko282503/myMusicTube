package com.musictube.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives [Intent.ACTION_BOOT_COMPLETED] and the vivo fast-boot equivalent
 * (QUICKBOOT_POWERON) after the device starts up.
 *
 * OEM ROMs (Xiaomi MIUI/HyperOS, OPPO ColorOS, vivo FuntouchOS) do NOT
 * automatically restart services that were alive before shutdown. This receiver
 * exists to hold the android.permission.RECEIVE_BOOT_COMPLETED grant and gives
 * the app a hook-point to restore state on reboot.
 *
 * Currently: validates the action and logs startup — no automatic playback
 * resume to avoid surprising the user. Extend this receiver when a
 * "resume playback on reboot" feature is added.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        android.util.Log.d("BootReceiver", "Device boot completed — app is ready")
        // Future: check shared prefs for "was playing" flag and restart MusicPlayerService
    }
}
