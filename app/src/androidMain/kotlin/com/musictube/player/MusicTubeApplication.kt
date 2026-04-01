package com.musictube.player

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.musictube.player.di.androidModule
import com.musictube.player.di.commonModule
import com.musictube.player.service.NewPipeDownloader
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit

class MusicTubeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize NewPipe extractor for YouTube stream extraction
        NewPipe.init(NewPipeDownloader.getInstance())

        // Initialize Koin dependency injection
        startKoin {
            androidContext(this@MusicTubeApplication)
            modules(commonModule, androidModule)
        }

        // Initialize Coil3 with OkHttp engine
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components {
                    add(OkHttpNetworkFetcherFactory(
                        callFactory = {
                            OkHttpClient.Builder()
                                .connectTimeout(15, TimeUnit.SECONDS)
                                .readTimeout(20, TimeUnit.SECONDS)
                                .build()
                        }
                    ))
                }
                .crossfade(true)
                .build()
        }
    }
}
