package com.musictube.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe

@HiltAndroidApp
class MusicTubeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize NewPipe Extractor for YouTube audio stream extraction
        NewPipe.init(com.musictube.player.service.NewPipeDownloader.getInstance())
    }
}