package com.musictube.player.di

import com.musictube.player.data.database.DatabaseHelper
import com.musictube.player.data.repository.MusicRepository
import com.musictube.player.service.SearchService
import com.musictube.player.service.SearchStateHolder
import com.musictube.player.service.YouTubeStreamService
import com.musictube.player.viewmodel.DownloadsViewModel
import com.musictube.player.viewmodel.HomeViewModel
import com.musictube.player.viewmodel.MainViewModel
import com.musictube.player.viewmodel.PlaylistViewModel
import com.musictube.player.viewmodel.PlayerViewModel
import com.musictube.player.viewmodel.QuickPicksViewModel
import com.musictube.player.viewmodel.SearchViewModel
import com.musictube.player.viewmodel.SharedPlaylistViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    // Database
    single { DatabaseHelper(get()) }

    // Repository
    single { MusicRepository(get()) }

    // Services
    single { SearchService() }
    single { YouTubeStreamService() }
    single { SearchStateHolder() }

    // ViewModels
    viewModel { MainViewModel(get(), get()) }
    viewModel { PlayerViewModel(get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { (playlistId: String) -> PlaylistViewModel(playlistId, get(), get(), get()) }
    viewModel { DownloadsViewModel(get()) }
    viewModel { QuickPicksViewModel(get(), get(), get()) }
    viewModel { SharedPlaylistViewModel(get(), get()) }
}
