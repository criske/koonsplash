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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import pcf.crskdev.koonsplash.http.HttpClient.withProgressListener
import java.io.StringReader
import kotlin.math.roundToInt

/**
 * API call.
 *
 */
interface ApiCall {

    /**
     * Invoker
     *
     * @param param Params
     * @return [ApiJsonResponse]
     */
    suspend operator fun invoke(vararg param: Param): ApiJsonResponse

    /**
     * Generic execution with observing the call progress.
     *
     * @param params Params
     * @param progressType Progress Type default [Progress.Ignore]
     * @param transformer Transforms a successful response to [T]
     * @return ProgressStatus stream
     */
    fun <T> execute(
        params: List<Param>,
        progressType: Progress = Progress.Ignore,
        transformer: (Response) -> T
    ): Flow<ProgressStatus<T>>

    /**
     * ApiJsonResponse execution with observing the call progress.
     *
     * @param params Params
     * @param progressType Progress Type default [Progress.Ignore]
     * @return ProgressStatus stream
     */
    fun execute(
        params: List<Param>,
        progressType: Progress = Progress.Ignore
    ): Flow<ProgressStatus<ApiJsonResponse>>

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

        /**
         * Progress will be shown by a step of skipped bytes.
         *
         * @property bytesToSkip Bytes to skip.
         * @constructor Create empty By step
         */
        class ByStep(val bytesToSkip: Long) : Progress() {

            init {
                require(bytesToSkip > 0) {
                    "Minimum step should at least 1 byte/"
                }
            }
        }
    }

    /**
     * Progress status tracker.
     *
     * @constructor Create empty Progress status
     */
    sealed class ProgressStatus<T> {
        class Canceled<T>(val err: Throwable) : ProgressStatus<T>()
        class Current<T>(val value: Number, val length: Long) : ProgressStatus<T>()
        class Done<T>(val resource: T) : ProgressStatus<T>()
        class Starting<T> : ProgressStatus<T>()
    }
}

/**
 * API call impl.
 *
 * @property httpClient Http client
 * @property accessKey Access key
 * @property authToken Auth Token, might be null for unauthenticated calls.
 */
internal class ApiCallImpl(
    private val endpoint: Endpoint,
    private val httpClient: OkHttpClient,
    private val accessKey: AccessKey,
    private val authToken: AuthToken?
) : ApiCall {

    private val endpointParser = EndpointParser(endpoint)

    @ExperimentalCoroutinesApi
    override suspend fun invoke(vararg param: Param): ApiJsonResponse = coroutineScope {
        val responseChannel = Channel<ApiJsonResponse>()
        execute(param.toList()).collect {
            when (it) {
                is ApiCall.ProgressStatus.Done<*> ->
                    launch {
                        responseChannel.send(it.resource as ApiJsonResponse)
                    }
                is ApiCall.ProgressStatus.Canceled ->
                    throw it.err
                else -> {
                }
            }
        }
        var receive = responseChannel.receive()
        receive
    }

    @ExperimentalCoroutinesApi
    override fun <T> execute(
        params: List<Param>,
        progressType: ApiCall.Progress,
        transformer: (Response) -> T
    ): Flow<ApiCall.ProgressStatus<T>> = callbackFlow {

        val url = endpoint.baseUrl.toHttpUrl().newBuilder()
            .apply {
                endpointParser.parse(*params.toTypedArray()).forEach {
                    when (it) {
                        is EndpointParser.Token.Path -> addPathSegment(it.value)
                        is EndpointParser.Token.QueryParam -> addQueryParameter(it.name, it.value)
                    }
                }
            }
            .build()
        val request = Request.Builder()
            .addKoonsplashHeaders()
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
        try {
            if (progressType != ApiCall.Progress.Ignore) {
                offer(ApiCall.ProgressStatus.Starting<T>())
            }
            val response = httpClient
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
                                                offer(
                                                    ApiCall
                                                        .ProgressStatus
                                                        .Current<T>(current, total)
                                                )
                                            }
                                        }
                                        ApiCall.Progress.Raw -> offer(
                                            ApiCall
                                                .ProgressStatus
                                                .Current<T>(read, total)
                                        )
                                        is ApiCall.Progress.ByStep -> if (read.rem(progressType.bytesToSkip) == 0) {
                                            offer(
                                                ApiCall
                                                    .ProgressStatus
                                                    .Current<T>(read, total)
                                            )
                                        }
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
            if (response.isSuccessful) {
                offer(ApiCall.ProgressStatus.Done(transformer(response)))
            } else {
                offer(
                    ApiCall.ProgressStatus.Canceled<T>(
                        IllegalStateException("Bad request [$url] ${response.code}: ${response.message}")
                    )
                )
            }
        } catch (ex: Exception) {
            offer(ApiCall.ProgressStatus.Canceled<T>(ex))
        } finally {
            close()
        }
        awaitClose {}
    }

    @ExperimentalCoroutinesApi
    override fun execute(
        params: List<Param>,
        progressType: ApiCall.Progress
    ): Flow<ApiCall.ProgressStatus<ApiJsonResponse>> =
        execute(params, progressType) { response ->
            val jsonReader = response.body?.charStream() ?: StringReader("{}")
            ApiJsonResponse(
                { link -> ApiCallImpl(Endpoint(link), httpClient, accessKey, authToken) },
                jsonReader,
                ApiMeta(response.headers)
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

    private fun Request.Builder.addKoonsplashHeaders(): Request.Builder =
        addHeader("Accept-Version", "v1")
            .addHeader("Authorization", "Client-ID $accessKey")
            .apply {
                if (authToken != null) {
                    addHeader("Authorization", "Bearer ${authToken.accessToken}")
                }
            }
}
