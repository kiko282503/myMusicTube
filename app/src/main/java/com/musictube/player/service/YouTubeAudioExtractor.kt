package com.musictube.player.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeAudioExtractor @Inject constructor() {

    companion object {
        private const val TAG = "YouTubeAudioExtractor"
    }

    /**
     * Extract direct audio stream URL from a YouTube video using NewPipe Extractor.
     * Returns the best-quality audio stream URL, or null if extraction fails.
     */
    suspend fun extractAudioUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Extracting audio via NewPipe for: ")

                val url = "https://www.youtube.com/watch?v=$videoId"
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()

                val audioStreams: List<AudioStream> = extractor.audioStreams
                if (audioStreams.isEmpty()) {
                    Log.w(TAG, "No audio streams found for: $videoId")
                    return@withContext null
                }

                // Pick highest bitrate audio stream
                val best = audioStreams.maxByOrNull { it.averageBitrate }
                val streamUrl = best?.content

                if (streamUrl != null) {
                    Log.i(TAG, "NewPipe extraction SUCCESS for: $videoId (bitrate: ${best.averageBitrate})")
                } else {
                    Log.w(TAG, "NewPipe returned null stream URL for: $videoId")
                }

                streamUrl
            } catch (e: Exception) {
                Log.e(TAG, "NewPipe extraction FAILED for: $videoId", e)
                null
            }
        }
    }

    /**
     * Extract audio URL using YouTube Music API specifically.
     * YouTube Music uses the same video IDs, so we just use the standard extractor.
     */
    suspend fun extractFromYouTubeMusic(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Extracting YouTube Music audio via NewPipe for: $videoId")

                val url = "https://music.youtube.com/watch?v=$videoId"
                // NewPipe recognises music.youtube.com URLs as YouTube service
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()

                val audioStreams: List<AudioStream> = extractor.audioStreams
                if (audioStreams.isEmpty()) {
                    Log.w(TAG, "No audio streams from YouTube Music for: $videoId")
                    return@withContext null
                }

                val best = audioStreams.maxByOrNull { it.averageBitrate }
                val streamUrl = best?.content

                if (streamUrl != null) {
                    Log.i(TAG, "YouTube Music NewPipe extraction SUCCESS for: $videoId")
                }

                streamUrl
            } catch (e: Exception) {
                Log.e(TAG, "YouTube Music NewPipe extraction FAILED for: $videoId", e)
                null
            }
        }
    }
}
