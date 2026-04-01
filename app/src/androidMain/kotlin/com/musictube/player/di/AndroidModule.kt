package com.musictube.player.di

import android.content.Context
import com.musictube.player.platform.AudioPlayerController
import com.musictube.player.platform.DatabaseDriverFactory
import com.musictube.player.platform.DownloadController
import com.musictube.player.service.AndroidDownloadManager
import com.musictube.player.service.LocalAudioManager
import com.musictube.player.service.MusicPlayerManager
import com.musictube.player.service.OkHttpDownloader
import com.musictube.player.service.YouTubeAudioExtractor
import org.koin.dsl.module

val androidModule = module {
    single { DatabaseDriverFactory(get<Context>()) }

    single { OkHttpDownloader(get<Context>()) }
    single { YouTubeAudioExtractor() }
    single { LocalAudioManager(get<Context>()) }

    single {
        MusicPlayerManager(
            context = get(),
            audioExtractor = get(),
            localAudioManager = get(),
            youTubeStreamService = get()
        )
    }
    single<AudioPlayerController> { get<MusicPlayerManager>() }

    single<DownloadController> {
        AndroidDownloadManager(
            context = get(),
            downloader = get(),
            youTubeStreamService = get(),
            searchService = get(),
            musicRepository = get()
        )
    }
}
