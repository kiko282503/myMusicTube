package com.musictube.player.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class OkHttpDownloader(private val context: Context) {
    private val tag = "OkHttpDownloader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun downloadAudio(
        url: String,
        fileNameHint: String,
        onProgress: (Int) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        fun buildRequest(withYoutubeHeaders: Boolean): Request {
            val builder = Request.Builder().url(url)
            if (withYoutubeHeaders) {
                builder
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                    .addHeader("Accept", "*/*")
                    .addHeader("Referer", "https://www.youtube.com/")
                    .addHeader("Origin", "https://www.youtube.com")
                    .addHeader("Range", "bytes=0-")
            }
            return builder.build()
        }

        var response = client.newCall(buildRequest(withYoutubeHeaders = false)).execute()
        if (!response.isSuccessful && (response.code == 401 || response.code == 403)) {
            Log.w(tag, "Primary download failed HTTP ${response.code}. Retrying with YouTube headers")
            response.close()
            response = client.newCall(buildRequest(withYoutubeHeaders = true)).execute()
        }
        if (!response.isSuccessful) {
            throw IOException("Download failed with HTTP ${response.code}")
        }
        val body = response.body ?: throw IOException("Empty response body")
        val total = body.contentLength()

        val downloadsDir = File(context.filesDir, "downloads").also { it.mkdirs() }
        val safeName = fileNameHint.lowercase().replace("[^a-z0-9._-]".toRegex(), "_").take(42)
        val targetFile = File(downloadsDir, "${safeName}_${UUID.randomUUID()}.m4a")
        Log.d(tag, "Saving to ${targetFile.absolutePath}")

        body.byteStream().use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(8 * 1024)
                var downloaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0) onProgress(((downloaded * 100L) / total).toInt().coerceIn(0, 100))
                }
                output.flush()
            }
        }
        targetFile.absolutePath
    }
}
