package com.musictube.player.ui.component

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.musictube.player.util.OemCompatHelper

/**
 * Dismissible banner shown on OEM devices (Xiaomi / OPPO / vivo / Huawei / Samsung)
 * that can kill background music services.
 *
 * Guides the user through two required manual steps:
 *   1. Exempt the app from Android battery optimization (standard API).
 *   2. Enable "Autostart" inside the OEM's own security / power manager.
 *
 * The banner hides itself once battery optimization is already exempted AND
 * the user has tapped Dismiss. The dismiss preference is persisted in
 * SharedPreferences so it never re-appears after the user has acted on it.
 */
@Composable
fun OemCompatBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Only show on OEM builds that restrict background services
    if (!OemCompatHelper.isAffectedOem) return

    val prefs = remember {
        context.getSharedPreferences("musictube_prefs", Context.MODE_PRIVATE)
    }

    var batteryOptIgnored by remember {
        mutableStateOf(OemCompatHelper.isIgnoringBatteryOptimizations(context))
    }
    var dismissed by remember {
        mutableStateOf(prefs.getBoolean("oem_banner_dismissed", false))
    }

    // Hide when fully configured and dismissed
    val visible = !dismissed || !batteryOptIgnored

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {

                // ── Title row ────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Background playback may be restricted",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            prefs.edit().putBoolean("oem_banner_dismissed", true).apply()
                            dismissed = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Description ───────────────────────────────────────────────
                val oemName = OemCompatHelper.oem.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
                Text(
                    text = "$oemName devices may stop music when the screen turns off. " +
                           "Enable both settings below to allow background playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(Modifier.height(8.dp))

                // ── Action buttons ────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Step 1 — Battery optimization (hidden once granted)
                    if (!batteryOptIgnored) {
                        val batteryLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.StartActivityForResult()
                        ) {
                            batteryOptIgnored = OemCompatHelper.isIgnoringBatteryOptimizations(context)
                        }
                        OutlinedButton(
                            onClick = {
                                batteryLauncher.launch(OemCompatHelper.batteryOptIntent(context))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text(
                                "Battery settings",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }

                    // Step 2 — OEM autostart settings
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(OemCompatHelper.autostartIntent(context))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(
                            "Autostart settings",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }

                // Hint label for the OEM-specific settings name
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "→ ${OemCompatHelper.autostartSettingsLabel()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
