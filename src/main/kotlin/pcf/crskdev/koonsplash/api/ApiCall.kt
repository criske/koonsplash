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

import kotlinx.coroutines.coroutineScope
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import java.io.StringReader

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

    private val baseUrl = HttpUrl.Builder()
        .scheme("https")
        .host("api.unsplash.com")
        .build()

    private val endpointParser = EndpointParser(baseUrl.toString(), endpoint.path)

    override suspend fun invoke(vararg param: Param): ApiJsonResponse = coroutineScope {
        val url = baseUrl.newBuilder()
            .apply {
                endpointParser.parse(*param).forEach {
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
        val response = httpClient.newCall(request).executeCo()
        if (response.isSuccessful) {
            val jsonReader = response.body?.charStream() ?: StringReader("{}")
            ApiJsonResponse(
                jsonReader,
                ApiMeta(response.headers)
            )
        } else {
            throw IllegalStateException("Bad request [$url] ${response.code}: ${response.message}")
        }
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
