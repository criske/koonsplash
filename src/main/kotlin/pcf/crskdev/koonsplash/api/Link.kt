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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
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
sealed class Link(protected val url: String) {

    override fun toString(): String = this.url

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
                "api.unsplash.com" -> {
                    if (uri.path?.endsWith("download") == true) {
                        Download(url, apiCall)
                    } else {
                        Api(url, apiCall)
                    }
                }
                "images.unsplash.com" -> Photo(url)
                else -> Browser(url)
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
    class Api(url: String, private val apiCall: (String) -> ApiCall) : Link(url) {

        /**
         * Calls the link URL.
         *
         * @return ApiJsonResponse
         */
        suspend fun call(): ApiJsonResponse = coroutineScope { apiCall(url)() }
    }

    /**
     * Show the link on the platform browser. Browser links are those links that are not part of
     * unsplash (external links like portfolio, author site etc...), or links that have their url
     * not part of _api.unsplash.com_ domain.
     *
     * It's up to client to launch this link in a browser. See [Browser.open]
     *
     * @constructor
     *
     * @param url Url
     */
    class Browser(url: String) : Link(url) {

        /**
         * Open the link in a platform browser.
         *
         * @param launcher Launching block
         * @receiver Launching block
         */
        suspend fun open(launcher: suspend (String) -> Unit) = coroutineScope {
            launch {
                launcher(url)
            }
        }
    }

    /**
     * Downloads a photo.
     *
     * @constructor
     *
     * @param url Photo link url.
     * @param apiCall [ApiCall] required for download the link.
     */
    class Download(url: String, private val apiCall: (String) -> ApiCall) : Link(url) {

        /**
         * Downloads a photo to a file.
         *
         * @param location
         * @param progressType
         * @param progress
         * @receiver
         */
        @ExperimentalCoroutinesApi
        fun download(
            location: File,
            progressType: ApiCall.Progress = ApiCall.Progress.Percent
        ): Flow<ApiCall.ProgressStatus<Photo>> = apiCall(url)
            .execute(emptyList(), progressType = progressType) { response ->
                FileOutputStream(location).use { fos ->
                    fos.write(response.body!!.bytes())
                }
                Photo(location.absolutePath)
            }
    }

    /**
     * Photo
     *
     * @constructor
     *
     * @param url
     */
    class Photo(url: String) : Link(url)
}
