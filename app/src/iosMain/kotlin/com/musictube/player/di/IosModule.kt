package com.musictube.player.di

import com.musictube.player.platform.AudioPlayerController
import com.musictube.player.platform.DatabaseDriverFactory
import com.musictube.player.platform.DownloadController
import com.musictube.player.platform.IosAudioPlayer
import com.musictube.player.platform.IosDownloadController
import org.koin.dsl.module

val iosModule = module {
    single { DatabaseDriverFactory() }
    single<AudioPlayerController> { IosAudioPlayer() }
    single<DownloadController> { IosDownloadController() }
}
