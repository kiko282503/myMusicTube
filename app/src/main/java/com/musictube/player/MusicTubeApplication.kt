package com.musictube.player

import android.app.Application
import com.musictube.player.service.NewPipeDownloader
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe

@HiltAndroidApp
class MusicTubeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize NewPipe Extractor with our HTTP downloader
        NewPipe.init(NewPipeDownloader.getInstance())
    }
}