package com.musictube.player

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MusicTubeApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}