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

package pcf.crskdev.koonsplash.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import pcf.crskdev.koonsplash.http.HttpClient
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/**
 * Representation of a url from json response.
 *
 * @property url URL
 * @author Cristian Pela
 * @since 0.1
 */
sealed class Link(val url: URI) {

    override fun toString(): String = this.url.toString()

    companion object {

        /**
         * Creates a link based on url.
         *
         * @param apiCall [ApiCall] required for opening links.
         * @param url URL
         * @return Link
         */
        fun create(apiCall: (String) -> ApiCall, url: String): Link {
            val uri = URI.create(url)
            return when (uri.authority) {
                HttpClient.apiBaseUrl.authority -> {
                    if (uri.path?.endsWith("download") == true) {
                        Download(uri, apiCall)
                    } else {
                        Api(uri, apiCall)
                    }
                }
                HttpClient.imagesBaseUrl.authority -> Photo(uri)
                else -> Browser(uri)
            }
        }
    }

    /**
     * Link pointing to api resource.
     *
     * @property apiCall [ApiCall] required for opening links
     * @constructor
     * @param url URL
     */
    class Api(url: URI, private val apiCall: (String) -> ApiCall) : Link(url) {

        /**
         * Calls the link URL.
         *
         * @return ApiJsonResponse
         */
        suspend fun call(): ApiJsonResponse = coroutineScope { apiCall(url.toString())() }
    }

    /**
     * Show the link on the platform browser. Browser links are those links that are not part of
     * unsplash (external links like portfolio, author site etc...), or links that have their url
     * not part of _api.unsplash.com_ domain.
     *
     * It's up to client to launch this link in a browser.
     *
     * @constructor
     *
     * @param url Url
     */
    class Browser(url: URI) : Link(url)

    /**
     * Downloads a photo.
     *
     * @constructor
     *
     * @param url Photo link url.
     * @param apiCall [ApiCall] required for download the link.
     */
    class Download(url: URI, private val apiCall: (String) -> ApiCall) : Link(url) {

        /**
         * Downloads a photo to a file.
         *
         * @param dir File location
         * @param fileName File name.
         * @param progressType ApiCall.Progress
         * @param dispatcher CoroutineDispatcher
         * @param bufferSize Download to disk buffer size in bytes.
         * @receiver
         */
        @FlowPreview
        @ExperimentalCoroutinesApi
        fun download(
            dir: File,
            fileName: String,
            progressType: ApiCall.Progress = ApiCall.Progress.Percent,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            bufferSize: Int = 1024,
        ): Flow<ApiCall.ProgressStatus<Photo>> {
            return apiCall(url.toString())
                .execute(emptyList(), ApiCall.Progress.Ignore)
                .flatMapConcat {
                    when (it) {
                        is ApiCall.ProgressStatus.Done -> {
                            val downloadUrl: String = it.resource["url"]()
                            apiCall(downloadUrl).execute(emptyList(), progressType) { response ->
                                val ext = response.contentType?.subtype ?: "jpg"
                                val file = File(dir, "$fileName.$ext")
                                response.stream.use { input ->
                                    val buffer = ByteArray(bufferSize)
                                    FileOutputStream(file).use { fos ->
                                        while (true) {
                                            val count = input.read(buffer)
                                            if (count < 0) {
                                                break
                                            }
                                            fos.write(buffer, 0, count)
                                        }
                                    }
                                    Link.Photo(file.toURI())
                                }
                            }
                        }
                        is ApiCall.ProgressStatus.Canceled -> flowOf(ApiCall.ProgressStatus.Canceled(it.err))
                        else -> throw IllegalStateException("Should not reach here: $it")
                    }
                }
                .flowOn(dispatcher)
        }
    }

    /**
     * Photo
     *
     * @constructor
     *
     * @param url
     */
    class Photo(url: URI) : Link(url)
}

/**
 * Download a photo to disk.
 *
 * @param dir File location
 * @param fileName File name.
 * @param dispatcher CoroutineDispatcher
 * @param bufferSize Download to disk buffer size in bytes.
 * @return Link to the saved photo.
 */
@ExperimentalCoroutinesApi
@FlowPreview
suspend fun Link.Download.downloadToPhoto(
    dir: File,
    fileName: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    bufferSize: Int = 1024,
): Link.Photo = coroutineScope {
    val channel = Channel<Link.Photo>()
    download(dir, fileName, ApiCall.Progress.Ignore, dispatcher, bufferSize)
        .collect {
            when (it) {
                is ApiCall.ProgressStatus.Done<*> ->
                    launch {
                        channel.send(it.resource as Link.Photo)
                    }
                is ApiCall.ProgressStatus.Canceled ->
                    throw it.err
                else -> {
                }
            }
        }
    channel.receive()
}
