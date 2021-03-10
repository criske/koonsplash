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
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import pcf.crskdev.koonsplash.http.HttpClient.withProgressListener
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import pcf.crskdev.koonsplash.internal.newBuilder
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.util.concurrent.CancellationException
import kotlin.math.roundToInt

/**
 * API call.
 *
 * @see Api.endpoint
 *
 */
interface ApiCall {

    /**
     * Invoker. Can be canceled.
     *
     * @param cancel Signal that cancel the call. Default null meaning that there is no cancellation.
     * @param params Params
     * @return [ApiJsonResponse]
     */
    suspend operator fun invoke(vararg params: Param, cancel: CancelSignal? = null): ApiJsonResponse

    /**
     * The actual call. Can be canceled.
     *
     * See also: [ApiCall.invoke]
     *
     * @param cancel Signal that cancel the call. Default null meaning that there is no cancellation.
     * @param params Params
     * @return [ApiJsonResponse]
     */
    suspend fun call(vararg params: Param, cancel: CancelSignal? = null): ApiJsonResponse =
        this.invoke(*params, cancel = cancel)

    /**
     * Generic call with observing the call progress. Can be canceled.
     *
     * @param params Params
     * @param progressType Progress Type default [Progress.Ignore]
     * @param cancel Signal that cancel the call. Default null meaning that there is no cancellation.
     * @param transformer Transforms a successful response stream to [T]
     * @return ProgressStatus stream
     */
    fun <T> callWithProgress(
        vararg params: Param,
        progressType: Progress = Progress.Percent,
        cancel: CancelSignal? = null,
        transformer: (Response) -> T
    ): Flow<Status<T>>

    /**
     * ApiJsonResponse call with observing the call progress. Can be canceled.
     *
     * @param params Params
     * @param progressType Progress Type default [Progress.Ignore]
     * @param cancel Signal that cancel the call. Default null meaning that there is no cancellation.
     * @return ProgressStatus stream
     */
    fun callWithProgress(
        vararg params: Param,
        progressType: Progress = Progress.Percent,
        cancel: CancelSignal? = null
    ): Flow<Status<ApiJsonResponse>>

    /**
     * Download progress visualization type.
     *
     */
    sealed class Progress {

        /**
         * Ignore watching the progress.
         *
         * @constructor Create empty Ignore
         */
        object Ignore : Progress()

        /**
         * Progress will be shown as percentage
         *
         * @constructor Create empty Percent
         */
        object Percent : Progress()

        /**
         * Progress will be shown byte by byte.
         *
         * @constructor Create empty Raw
         */
        object Raw : Progress()
    }

    /**
     * Progress status tracker.
     *
     * @constructor Create empty Progress status
     */
    sealed class Status<T> {
        class Canceled<T>(val err: Throwable) : Status<T>()
        class Current<T>(val value: Number, val length: Long) : Status<T>()
        class Done<T>(val resource: T) : Status<T>()
        class Starting<T> : Status<T>()
    }

    /**
     * Abstracts the response.
     *
     * @constructor Create empty Response
     */
    interface Response {

        /**
         * Response body as byte stream.
         */
        val stream: InputStream

        /**
         * Response body as char reader.
         */
        val reader: Reader

        /**
         * Headers multi map
         */
        val headers: Headers

        /**
         * Content type
         */
        val contentType: ContentType?

        /**
         * Content type representation.
         *
         * @property type Type
         * @property subtype Sub type
         * @constructor Create empty Content type
         */
        data class ContentType(val type: String, val subtype: String)
    }
}

/**
 * API call impl.
 *
 * @property httpClient Http client
 * @property context KoonsplashContext.
 */
internal class ApiCallImpl(
    private val endpoint: Endpoint,
    private val httpClient: OkHttpClient,
    private val context: KoonsplashContext
) : ApiCall {

    private val endpointParser = EndpointParser(endpoint)

    @ExperimentalCoroutinesApi
    override suspend fun invoke(vararg params: Param, cancel: CancelSignal?): ApiJsonResponse =
        if (cancel == null) {
            callInternal(*params)
        } else {
            cancelableCallInternal(*params, cancel = cancel)
        }

    /**
     * Call without canceling.
     *
     * @param param Params
     * @return
     */
    @ExperimentalCoroutinesApi
    private suspend fun callInternal(vararg param: Param): ApiJsonResponse = coroutineScope {
        produce(coroutineContext) {
            callWithProgress(*param).collect {
                when (it) {
                    is ApiCall.Status.Done<*> ->
                        send(it.resource as ApiJsonResponse)
                    is ApiCall.Status.Canceled ->
                        throw it.err
                    else -> {
                    }
                }
            }
        }.receive()
    }

    /**
     * Call with canceling signal.
     *
     * @param param Params
     * @param cancel Signals that this request should be canceled
     * @return ApiJsonResponse or null if request was canceled.
     */
    @ExperimentalCoroutinesApi
    private suspend fun cancelableCallInternal(
        vararg param: Param,
        cancel: CancelSignal
    ): ApiJsonResponse = coroutineScope {
        val deferred = async { callInternal(*param) }
        val cancelJob = launch {
            cancel.collect {
                deferred.cancel()
            }
        }
        deferred.invokeOnCompletion {
            cancelJob.cancel()
        }
        deferred.await()
    }

    @ExperimentalCoroutinesApi
    private fun <T> cancelableCallWithProgressInternal(
        vararg params: Param,
        progressType: ApiCall.Progress,
        cancel: CancelSignal,
        transformer: (ApiCall.Response) -> T
    ): Flow<ApiCall.Status<T>> = channelFlow {
        val provideJob = launch {
            callWithProgressInternal(*params, progressType = progressType, transformer = transformer).collect {
                send(it)
                val isFinished = it is ApiCall.Status.Canceled || it is ApiCall.Status.Done
                if (isFinished) {
                    close()
                }
            }
        }
        val cancelJob = launch {
            cancel.collect {
                send(ApiCall.Status.Canceled<T>(CancellationException("Manually canceled")))
                provideJob.cancel()
                close()
            }
        }
        awaitClose {
            cancelJob.cancel()
        }
    }

    @ExperimentalCoroutinesApi
    private fun <T> callWithProgressInternal(
        vararg params: Param,
        progressType: ApiCall.Progress,
        transformer: (ApiCall.Response) -> T
    ): Flow<ApiCall.Status<T>> = callbackFlow {

        val url = endpoint.baseUrl.toHttpUrl().newBuilder()
            .apply {
                endpointParser.parse(*params).forEach {
                    when (it) {
                        is EndpointParser.Token.Path -> addPathSegment(it.value)
                        is EndpointParser.Token.QueryParam -> addQueryParameter(it.name, it.value)
                    }
                }
            }
            .build()
        val token = context.auth.getToken()
        val request = Request.Builder()
            .addKoonsplashHeaders(token, context.auth.accessKey)
            .url(url)
            .apply {
                when (endpoint.verb) {
                    Verb.Get -> get()
                    is Verb.Modify.Post -> post(createModifyForm(endpoint.verb))
                    is Verb.Modify.Put -> put(createModifyForm(endpoint.verb))
                    Verb.Delete -> delete()
                }
            }
            .build()

        var response: Response? = null
        try {
            if (progressType != ApiCall.Progress.Ignore) {
                offer(ApiCall.Status.Starting<T>())
            }
            response = httpClient
                .run {
                    if (progressType == ApiCall.Progress.Ignore) {
                        this
                    } else {
                        this.newBuilder()
                            .withProgressListener { read, lastRead, total, done ->
                                if (!done) {
                                    when (progressType) {
                                        ApiCall.Progress.Percent -> {
                                            val totalF = total.toFloat()
                                            val current = ((read / totalF) * 100).roundToInt()
                                            val last = ((lastRead / totalF) * 100).roundToInt()
                                            if (current != last) {
                                                offer(ApiCall.Status.Current<T>(current, total))
                                            }
                                        }
                                        ApiCall.Progress.Raw -> offer(
                                            ApiCall
                                                .Status
                                                .Current<T>(read, total)
                                        )
                                        else -> {
                                            throw IllegalStateException("Unknown ApiCall progress")
                                        }
                                    }
                                }
                            }
                            .build()
                    }
                }
                .newCall(request)
                .executeCo()
            launch {
                try {
                    offer(ApiCall.Status.Done(transformer(ResponseOkHttpWrapper(response))))
                } catch (ex: Exception) {
                    offer(ApiCall.Status.Canceled<T>(ex))
                } finally {
                    close()
                }
            }
        } catch (ex: Exception) {
            offer(ApiCall.Status.Canceled<T>(ex))
            close()
        }
        awaitClose {
            response?.body?.closeQuietly()
        }
    }

    @ExperimentalCoroutinesApi
    override fun <T> callWithProgress(
        vararg params: Param,
        progressType: ApiCall.Progress,
        cancel: CancelSignal?,
        transformer: (ApiCall.Response) -> T
    ): Flow<ApiCall.Status<T>> = if (cancel == null) {
        callWithProgressInternal(
            *params,
            progressType = progressType,
            transformer = transformer
        )
    } else {
        cancelableCallWithProgressInternal(
            *params,
            progressType = progressType,
            cancel = cancel,
            transformer = transformer
        )
    }

    @ExperimentalCoroutinesApi
    override fun callWithProgress(
        vararg params: Param,
        progressType: ApiCall.Progress,
        cancel: CancelSignal?
    ): Flow<ApiCall.Status<ApiJsonResponse>> =
        callWithProgress(*params, progressType = progressType, cancel = cancel) { response ->
            val jsonReader = response.reader
            val contextWithApiCaller = context.newBuilder()
                .apiCaller { ApiCallImpl(Endpoint(it), httpClient, context) }
                .build()
            ApiJsonResponse(
                contextWithApiCaller,
                jsonReader,
                response.headers
            )
        }

    private fun createModifyForm(verb: Verb.Modify): RequestBody =
        FormBody.Builder()
            .apply {
                verb.formEntries.forEach {
                    add(it.first, it.second)
                }
            }
            .build()

    private fun Request.Builder.addKoonsplashHeaders(token: AuthToken?, accessKey: AccessKey): Request.Builder =
        addHeader("Accept-Version", "v1")
            .addHeader("Authorization", "Client-ID $accessKey")
            .apply {
                if (token != null) {
                    addHeader("Authorization", "Bearer ${token.accessToken}")
                }
            }

    private class ResponseOkHttpWrapper(response: Response) : ApiCall.Response {

        override val stream: InputStream = response.body?.byteStream()
            ?: object : InputStream() {
                override fun read(): Int = -1
            }
        override val reader: Reader = response.body?.charStream() ?: StringReader("{}")
        override val headers: Headers = response.headers.toMultimap()
        override val contentType: ApiCall.Response.ContentType? =
            response.body?.contentType()?.let {
                ApiCall.Response.ContentType(it.type, it.subtype)
            }
    }
}
