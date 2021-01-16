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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import pcf.crskdev.koonsplash.api.resize.ResizeDSL
import pcf.crskdev.koonsplash.api.resize.ResizeException
import pcf.crskdev.koonsplash.api.resize.withResize
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import java.io.File
import java.io.FileOutputStream
import java.net.URI

/**
 * Representation of a url from json response.
 *
 * @property url URL
 * @property context KoonsplashContext.
 * @author Cristian Pela
 * @since 0.1
 */
sealed class Link(val url: URI, internal val context: KoonsplashContext) {

    override fun toString(): String = this.url.toString()

    companion object {

        /**
         * Creates a link based on url.
         *
         * @param url URL
         * @param context KoonsplashContext
         * @return Link
         */
        @PublishedApi
        internal fun create(url: String, context: KoonsplashContext): Link {
            val uri = URI.create(url)
            return when (uri.authority) {
                HttpClient.apiBaseUrl.authority -> {
                    if (uri.path?.endsWith("download") == true) {
                        Download(uri, context = context)
                    } else {
                        Api(uri, context)
                    }
                }
                HttpClient.imagesBaseUrl.authority -> Photo(uri, context)
                else -> Browser(uri, context)
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
    class Api internal constructor(url: URI, context: KoonsplashContext) : Link(url, context) {

        /**
         * Calls the link URL.
         *
         * @return ApiJsonResponse
         */
        suspend fun call(): ApiJsonResponse = coroutineScope { context.apiCaller(url.toString())() }
    }

    /**
     * Show the link on the platform browser. Browser links are those links that are not part of
     * unsplash (external links like portfolio, author site etc...), or links that have their url
     * not part of _api.unsplash.com_ domain.
     *
     * The links can be opened in a browser or, depending on url schema, in the os default photo app.
     *
     * @constructor
     *
     * @param url Url
     * @param context KoonsplashContext
     */
    class Browser internal constructor(url: URI, context: KoonsplashContext) : Link(url, context) {

        /**
         * Open the wrapped link.
         *
         */
        fun open() {
            this.context.browserLauncher.launch(url)
        }
    }

    /**
     * Downloads a photo.
     *
     * @constructor
     *
     * @param url Photo link url.
     * @param context: KoonsplashContext
     */
    class Download internal constructor(
        url: URI,
        private val policy: Policy = Policy.UNSPLASH,
        context: KoonsplashContext
    ) : Link(url, context) {

        enum class Policy {
            UNSPLASH, IMGIX
        }

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
        fun downloadWithProgress(
            dir: File,
            fileName: String,
            progressType: ApiCall.Progress = ApiCall.Progress.Percent,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            bufferSize: Int = 1024,
        ): Flow<ApiCall.Status<Browser>> {
            return when (policy) {
                Policy.UNSPLASH ->
                    this.context.apiCaller(url.toString())
                        .execute(emptyList(), ApiCall.Progress.Ignore)
                        .flatMapConcat {
                            when (it) {
                                is ApiCall.Status.Done -> {
                                    val downloadUrl: String = it.resource["url"]()
                                    this.context.apiCaller(downloadUrl).execute(emptyList(), progressType) { response ->
                                        save(response, dir, fileName, bufferSize)
                                    }
                                }
                                is ApiCall.Status.Canceled -> flowOf(ApiCall.Status.Canceled(it.err))
                                else -> throw IllegalStateException("Should not reach here: $it")
                            }
                        }
                        .flowOn(dispatcher)
                Policy.IMGIX ->
                    this.context.apiCaller(this.url.toString()).execute(emptyList(), progressType) { response ->
                        save(response, dir, fileName, bufferSize)
                    }.flowOn(dispatcher)
            }
        }

        /**
         * Save file to disk.
         *
         * @param response
         * @param dir
         * @param fileName
         * @param bufferSize
         * @return Browser Link.
         */
        private fun save(
            response: ApiCall.Response,
            dir: File,
            fileName: String,
            bufferSize: Int
        ): Link.Browser {
            val ext = response.contentType?.subtype ?: "jpg"
            val file = File(dir, "$fileName.$ext")
            return response.stream.use { input ->
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
                Browser(file.toURI(), this.context)
            }
        }
    }

    /**
     * Photo
     *
     * @constructor
     *
     * @param url
     * @param context
     */
    class Photo internal constructor(url: URI, context: KoonsplashContext) :
        Link(url, context) {

        /**
         * Dynamically resizable Photo dsl entry point.
         *
         * [See more](https://unsplash.com/documentation#dynamically-resizable-images)
         * @param dslScopeBlock Resize scope.
         */
        @ExperimentalUnsignedTypes
        fun resize(from: ResizeDSL = ResizeDSL.NONE, dslScopeBlock: ResizeDSL.Scope.() -> Unit = {}): Resize {
            return Resize(this.url, withResize(from, dslScopeBlock), this.context)
        }

        /**
         * Resize logic controller.
         *
         * @property checked Flag that marks if baseUrl was checked for having "ixid" parameters.
         * @property dsl ResizeDSL.
         * @property baseUrl Initial url
         * @property context KoonsplashContext
         * @constructor Create empty Resize
         */
        @ExperimentalUnsignedTypes
        class Resize private constructor(
            private val checked: Boolean,
            private val baseUrl: URI,
            dsl: ResizeDSL,
            private val context: KoonsplashContext
        ) {

            /**
             * Constructor with unchecked baseUrl.
             */
            internal constructor(
                baseUrl: URI,
                dsl: ResizeDSL,
                context: KoonsplashContext
            ) : this(false, baseUrl, dsl, context)

            /**
             * Merged dsl fom baseUrl and the passed dsl.
             */
            private val mergedDSL: ResizeDSL

            init {
                if (!checked && baseUrl.query?.contains("ixid=") != true) {
                    throw ResizeException("Url $baseUrl can't be used for image resize, it must contain `ixid` parameter")
                }
                mergedDSL = ResizeDSL.fromUrl(baseUrl).merge(dsl)
            }

            /**
             * As download link.
             *
             * @return Link
             */
            fun asDownloadLink(): Download = Link.Download(
                this.url(),
                Download.Policy.IMGIX,
                this.context
            )

            /**
             * As photo link.
             *
             * @return Link.
             */
            fun asPhotoLink(): Photo = Link.Photo(this.url(), this.context)

            /**
             * Add new Resize dsl this one.
             *
             * @param dslScopeBlock
             * @receiver
             * @return New Resize.
             */
            fun add(dslScopeBlock: ResizeDSL.Scope.() -> Unit): Resize =
                Resize(
                    true,
                    this.baseUrl,
                    withResize(this.mergedDSL, dslScopeBlock),
                    this.context
                )

            /**
             * Reset the resizing, all the Resize construct will be lost.
             *
             * @param dslScopeBlock New Resize dsl block.
             * @receiver
             * @return New Resize.
             */
            fun reset(dslScopeBlock: ResizeDSL.Scope.() -> Unit = {}): Resize {
                val ixid = this.baseUrl.toHttpUrlOrNull()!!.queryParameter("ixid")!!
                val resetBaseUrl = this.baseUrl.toString()
                    .let { it.substring(0, it.indexOf("?")) }
                    .toHttpUrlOrNull()!!
                    .newBuilder()
                    .addQueryParameter("ixid", ixid)
                    .build().toUri()
                return Resize(
                    true,
                    resetBaseUrl,
                    withResize(scope = dslScopeBlock),
                    this.context
                )
            }

            /**
             * Url created from baseUrl and Resize query parameters.
             *
             * @return URI full url.
             */
            private fun url(): URI =
                this.baseUrl
                    .toHttpUrlOrNull()!!
                    .newBuilder()
                    .apply {
                        mergedDSL().forEach { (key, value) ->
                            setEncodedQueryParameter(key, value)
                        }
                    }
                    .build()
                    .toUri()
        }
    }
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
suspend fun Link.Download.download(
    dir: File,
    fileName: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    bufferSize: Int = 1024,
): Link.Browser = coroutineScope {
    val channel = Channel<Link.Browser>()
    downloadWithProgress(dir, fileName, ApiCall.Progress.Ignore, dispatcher, bufferSize)
        .collect {
            when (it) {
                is ApiCall.Status.Done<*> ->
                    launch {
                        channel.send(it.resource as Link.Browser)
                    }
                is ApiCall.Status.Canceled ->
                    throw it.err
                else -> {
                }
            }
        }
    channel.receive()
}
