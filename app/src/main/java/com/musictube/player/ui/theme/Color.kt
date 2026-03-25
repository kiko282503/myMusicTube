package com.musictube.player.ui.theme

import androidx.compose.ui.graphics.Color

// ── Backgrounds (soft light purple — easy on the eyes) ─────────
val DeepPurpleBlack     = Color(0xFFF0E8FF)   // main app background — very pale lavender
val DarkPurpleSurface   = Color(0xFFE8DAFF)   // card / bottom sheet background
val ElevatedPurple      = Color(0xFFDDCDFF)   // elevated cards, dialogs
val SurfaceVariantColor = Color(0xFFCFBEF5)   // chips, text-field background

// ── Primary (medium purple — stands out against light background) ──
val VibrantPurple       = Color(0xFF7C3AED)   // primary action colour
val PurpleContainer     = Color(0xFFD8B4FE)   // primary container (soft)
val LightLavender       = Color(0xFF3B0764)   // on primary container (dark text)
val NeonPurpleGlow      = Color(0xFF9D5CF6)   // lighter variant / highlights

// ── Secondary ────────────────────────────────────────────────
val MutedViolet         = Color(0xFF6D5B8A)   // secondary
val SecondaryContainer  = Color(0xFFE9DDFF)   // secondary container
val OnSecondaryContain  = Color(0xFF1D0048)   // on secondary container

// ── Tertiary (pink accent — hearts, likes, active badge) ──────
val PinkAccent          = Color(0xFFBE185D)   // tertiary — deeper pink for legibility on light bg
val PinkContainer       = Color(0xFFFFD6E7)   // tertiary container
val OnPinkContainer     = Color(0xFF3E0020)   // on tertiary container

// ── Text / on-colours (dark for legibility on light background) ──
val WarmWhite           = Color(0xFF1E0643)   // on background — deep purple-black
val SoftLavenderText    = Color(0xFF2D1B69)   // on surface — dark purple
val DimSubtleText       = Color(0xFF6B5A8A)   // on surface variant / secondary text
val OutlineColor        = Color(0xFF9C85C4)   // borders, dividers
val OutlineVariant      = Color(0xFFCBB8F0)   // subtle borders

// ── Error ────────────────────────────────────────────────────
val ErrorRed            = Color(0xFFB91C1C)
val ErrorContainer      = Color(0xFFFFE4E4)
val OnError             = Color(0xFFFFFFFF)
val OnErrorContainer    = Color(0xFF7F1D1D)

// ── Legacy aliases (used in Theme.kt and across the codebase) ─
val Purple80            = Color(0xFFD0BCFF)
val PurpleGrey80        = Color(0xFFCCC2DC)
val Pink80              = Color(0xFFEFB8C8)
val Purple40            = Color(0xFF6650a4)
val PurpleGrey40        = Color(0xFF625b71)
val Pink40              = Color(0xFF7D5260)

val MusicPrimary        = VibrantPurple
val MusicPrimaryVariant = NeonPurpleGlow
val MusicSecondary      = DarkPurpleSurface
val MusicBackground     = DeepPurpleBlack
val MusicSurface        = DarkPurpleSurface
val MusicOnPrimary      = Color.White
val MusicOnSecondary    = WarmWhite
val MusicOnBackground   = WarmWhite
val MusicOnSurface      = SoftLavenderText