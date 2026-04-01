package com.musictube.player

import androidx.compose.ui.window.ComposeUIViewController
import com.musictube.player.di.commonModule
import com.musictube.player.di.iosModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(commonModule, iosModule)
    }
}

fun MainViewController() = ComposeUIViewController { App() }
