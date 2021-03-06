/*
 *  Copyright (c) 2020. Pela Cristian
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 *  DEALINGS IN THE SOFTWARE.
 */

package pcf.crskdev.koonsplash.http

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Http client based on OkHttp.
 * @author Cristian Pela
 * @since 0.1
 */
internal object HttpClient {

    var apiBaseUrl = URI.create("https://api.unsplash.com/")

    var imagesBaseUrl = URI.create("https://images.unsplash.com/")

    var baseUrl = URI.create("https://unsplash.com/")

    val http = OkHttpClient.Builder()
        .followRedirects(true)
        .cookieJar(
            JavaNetCookieJar(
                CookieManager().apply { setCookiePolicy(CookiePolicy.ACCEPT_ALL) }
            )
        )
        .build()

    /**
     * Coroutine equivalent of Call#execute()
     *
     */
    suspend fun Call.executeCo(): Response = suspendCancellableCoroutine { cont ->
        this@executeCo.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    cont.resume(response)
                } else {
                    cont.resumeWithException(HttpException(response.code, response.message))
                }
            }
        })
        cont.invokeOnCancellation {
            this@executeCo.cancel()
        }
    }

    /**
     * Progress response body.
     *
     * @property responseBody ResponseBody
     * @property progressListener Callback
     * @constructor Create empty Progress response body
     */
    private class ProgressResponseBody(
        private val responseBody: ResponseBody,
        private val progressListener: ProgressListener
    ) : ResponseBody() {
        private var bufferedSource: BufferedSource? = null

        override fun contentType(): MediaType? {
            return responseBody.contentType()
        }

        override fun contentLength(): Long {
            return responseBody.contentLength()
        }

        override fun source(): BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody.source()).buffer()
            }
            return bufferedSource as BufferedSource
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                var totalBytesRead = 0L
                var lastBytesRead: Long = 0L
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    progressListener(totalBytesRead, lastBytesRead, responseBody.contentLength(), bytesRead == -1L)
                    lastBytesRead = totalBytesRead
                    return bytesRead
                }
            }
        }
    }

    fun OkHttpClient.Builder.withProgressListener(listener: ProgressListener) =
        addNetworkInterceptor { chain ->
            val originalResponse: Response = chain.proceed(chain.request())
            originalResponse.newBuilder()
                .body(originalResponse.body?.let { ProgressResponseBody(it, listener) })
                .build()
        }
}

typealias ProgressListener = (Long, Long, Long, Boolean) -> Unit

class HttpException(val code: Int, override val message: String) : RuntimeException(message)
