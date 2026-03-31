package com.musictube.player.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for OEM-specific battery and autostart compatibility.
 *
 * Xiaomi (MIUI/HyperOS), OPPO (ColorOS), vivo (FuntouchOS/OriginOS),
 * Huawei (EMUI), Realme, and OnePlus all ship proprietary background-process
 * managers that can silently kill the music service even while playing.
 *
 * Typical two-step fix for affected devices:
 *   1. Exempt from Android's standard battery optimization (ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).
 *   2. Enable "Autostart" / "Background launch" inside the OEM's own security app.
 *
 * Usage:
 *   if (OemCompatHelper.needsOemSetup(context)) {
 *       startActivity(OemCompatHelper.batteryOptIntent(context))   // step 1
 *       startActivity(OemCompatHelper.autostartIntent(context))    // step 2
 *   }
 */
object OemCompatHelper {

    enum class OemType {
        XIAOMI,   // MIUI / HyperOS
        OPPO,     // ColorOS (also covers Realme / narzo)
        VIVO,     // FuntouchOS / OriginOS
        HUAWEI,   // EMUI / HarmonyOS
        ONEPLUS,  // OxygenOS (older) / ColorOS (newer)
        SAMSUNG,  // One UI
        UNKNOWN
    }

    /** Detected manufacturer, resolved once at runtime. */
    val oem: OemType by lazy {
        when {
            Build.MANUFACTURER.equals("xiaomi",  ignoreCase = true) ||
            Build.MANUFACTURER.equals("redmi",   ignoreCase = true) ||
            Build.MANUFACTURER.equals("poco",    ignoreCase = true) -> OemType.XIAOMI

            Build.MANUFACTURER.equals("oppo",    ignoreCase = true) ||
            Build.MANUFACTURER.equals("realme",  ignoreCase = true) -> OemType.OPPO

            Build.MANUFACTURER.equals("vivo",    ignoreCase = true) ||
            Build.MANUFACTURER.equals("iqoo",    ignoreCase = true) -> OemType.VIVO

            Build.MANUFACTURER.equals("huawei",  ignoreCase = true) ||
            Build.MANUFACTURER.equals("honor",   ignoreCase = true) -> OemType.HUAWEI

            Build.MANUFACTURER.equals("oneplus", ignoreCase = true) -> OemType.ONEPLUS

            Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> OemType.SAMSUNG

            else -> OemType.UNKNOWN
        }
    }

    /**
     * True when the device is running an OEM ROM known to aggressively restrict
     * background services (music stopping, downloads pausing, etc.).
     */
    val isAffectedOem: Boolean get() = oem != OemType.UNKNOWN

    /** True when the app is already exempt from battery optimization. */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Returns true when the user still needs to take manual OEM-specific actions
     * to ensure background playback is not killed.
     */
    fun needsOemSetup(context: Context): Boolean =
        isAffectedOem && !isIgnoringBatteryOptimizations(context)

    /**
     * Intent that opens the system battery-optimization exemption dialog for this
     * app. This is an Android standard (API 23+) and works on all manufacturers.
     */
    fun batteryOptIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    /**
     * Intent to open the OEM-specific autostart / background-launch manager.
     *
     * Multiple candidate components are tried in priority order (most specific
     * to the installed ROM variant first). Falls back to the generic
     * App Info settings page when none of the OEM components are found.
     */
    fun autostartIntent(context: Context): Intent {
        val candidates: List<Intent> = when (oem) {
            // ── Xiaomi / Redmi / POCO ─────────────────────────────────────────────
            OemType.XIAOMI -> listOf(
                // MIUI 10+ / HyperOS Security app → Autostart section
                componentIntent("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                // Fallback: Security app main screen
                componentIntent("com.miui.securitycenter",
                    "com.miui.securitycenter.MainActivity")
            )
            // ── OPPO / Realme ─────────────────────────────────────────────────────
            OemType.OPPO -> listOf(
                // ColorOS 12+ (newer OPPO / Realme)
                componentIntent("com.oplus.safecenter",
                    "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
                // ColorOS 7–11
                componentIntent("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                // Older ColorOS
                componentIntent("com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"),
                // Power-manager fallback
                componentIntent("com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity")
            )
            // ── vivo / iQOO ───────────────────────────────────────────────────────
            OemType.VIVO -> listOf(
                // FuntouchOS / OriginOS
                componentIntent("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                // iQOO variant
                componentIntent("com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                // Application Behavior Engine (ABE) - older vivo
                componentIntent("com.vivo.abe",
                    "com.vivo.applicationbehaviorengine.ui.AppStartupActivity")
            )
            // ── Huawei / Honor ────────────────────────────────────────────────────
            OemType.HUAWEI -> listOf(
                // EMUI 9+ Startup Manager
                componentIntent("com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                // EMUI 8 Protect
                componentIntent("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity")
            )
            // ── OnePlus ───────────────────────────────────────────────────────────
            OemType.ONEPLUS -> listOf(
                // OxygenOS (older OnePlus)
                componentIntent("com.oneplus.security",
                    "com.oneplus.security.AutoStartActivity"),
                // OxygenOS 13 / ColorOS (newer OnePlus)
                componentIntent("com.oplus.safecenter",
                    "com.oplus.safecenter.permission.startup.StartupAppListActivity")
            )
            // ── Samsung ───────────────────────────────────────────────────────────
            OemType.SAMSUNG -> listOf(
                // Device Care - Battery - Background usage limits
                Intent("com.samsung.android.sm.ACTION_APP_WHITELIST")
                    .setPackage("com.samsung.android.sm"),
                Intent("com.samsung.android.sm_cn.ACTION_APP_WHITELIST")
                    .setPackage("com.samsung.android.sm_cn")
            )
            OemType.UNKNOWN -> emptyList()
        }

        // Return the first candidate whose Activity actually exists on this device
        val pm = context.packageManager
        val resolved = candidates.firstOrNull { intent ->
            pm.resolveActivity(intent, 0) != null
        }
        return resolved ?: fallbackAppInfoIntent(context)
    }

    /** Human-readable name of the OEM settings screen (for UI copy). */
    fun autostartSettingsLabel(): String = when (oem) {
        OemType.XIAOMI  -> "Autostart  (MIUI Security app)"
        OemType.OPPO    -> "Auto-launch  (ColorOS App Management)"
        OemType.VIVO    -> "Background Open  (Permission Manager)"
        OemType.HUAWEI  -> "Startup Manager  (EMUI/HarmonyOS)"
        OemType.ONEPLUS -> "Auto-launch  (OxygenOS)"
        OemType.SAMSUNG -> "Device Care › Battery › Background usage limits"
        OemType.UNKNOWN -> "Auto-start manager"
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private fun componentIntent(pkg: String, cls: String): Intent =
        Intent().setComponent(ComponentName(pkg, cls))

    private fun fallbackAppInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
}
