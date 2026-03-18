package com.musictube.player.service

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NewPipeRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

/**
 * HTTP Downloader implementation for NewPipe Extractor.
 * NewPipe requires this bridge to make its internal HTTP requests.
 */
class NewPipeDownloader private constructor(private val client: OkHttpClient) : Downloader() {

    companion object {
        private const val TAG = "NewPipeDownloader"

        @Volatile
        private var instance: NewPipeDownloader? = null

        fun getInstance(): NewPipeDownloader {
            return instance ?: synchronized(this) {
                instance ?: NewPipeDownloader(
                    OkHttpClient.Builder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build()
                ).also { instance = it }
            }
        }
    }

    @Throws(ReCaptchaException::class, Exception::class)
    override fun execute(request: NewPipeRequest): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        Log.d(TAG, "Downloading: $url")

        val requestBuilder = Request.Builder().url(url)

        // Add headers
        headers.forEach { (name, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        // Set method and body
        val body = dataToSend?.toRequestBody("application/json".toMediaType())
        when (httpMethod) {
            "POST" -> requestBuilder.post(body ?: "".toRequestBody())
            "PUT"  -> requestBuilder.put(body ?: "".toRequestBody())
            "PATCH" -> requestBuilder.patch(body ?: "".toRequestBody())
            "DELETE" -> requestBuilder.delete(body)
            else -> requestBuilder.get()
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseHeaders = mutableMapOf<String, MutableList<String>>()
        response.headers.forEach { (name, value) ->
            responseHeaders.getOrPut(name) { mutableListOf() }.add(value)
        }

        return Response(
            response.code,
            response.message,
            responseHeaders,
            response.body?.string(),
            response.request.url.toString()
        )
    }
}
