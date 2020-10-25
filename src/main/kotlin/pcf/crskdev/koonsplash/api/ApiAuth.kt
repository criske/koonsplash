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

import okhttp3.OkHttpClient
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.http.HttpClient

/**
 * Api authenticated endpoints.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface ApiAuth : Api {

    suspend fun me()
}

/**
 * Api auth implementation.
 *
 * @property httpClient Http client.
 * @property accessKey Access key.
 * @property authToken Auth token.
 * @constructor
 * @param api Delegates unauthenticated API.
 */
class ApiAuthImpl(
    api: Api,
    private val httpClient: OkHttpClient,
    private val accessKey: AccessKey,
    private val authToken: AuthToken
) : ApiAuth, Api by api {

    override suspend fun me() {
        TODO("Not yet implemented")
    }

    override fun call(endpoint: String, verb: Verb): ApiCall =
        ApiCallImpl(Endpoint(HttpClient.apiBaseUrl.toString(), endpoint, verb), httpClient, accessKey, authToken)
}
