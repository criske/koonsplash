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

package pcf.crskdev.koonsplash.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import pcf.crskdev.koonsplash.http.HttpClient
import java.net.URI

/**
 * Authorizer implementation for authenticated API requests.
 * @property apiCall Auth Api call for token.
 **/
internal class AuthorizerImpl(
    private val apiCall: AuthApiCall,
    private val server: AuthCodeServer,
) : Authorizer {

    @ExperimentalCoroutinesApi
    override suspend fun authorize(
        accessKey: AccessKey,
        secretKey: SecretKey,
        scopes: AuthScope,
        browserLauncher: (URI) -> Unit
    ): AuthToken = coroutineScope {
        if (!server.startServing()) {
            throw IllegalStateException("Auth code server hasn't started")
        }

        val channel = produce(coroutineContext) {
            server.onAuthorizeCode {
                offer(it)
                close()
            }
            val browserUrl = HttpClient.baseUrl.toHttpUrlOrNull()!!
                .newBuilder()
                .addPathSegment("oauth")
                .addPathSegment("authorize")
                .addQueryParameter("client_id", accessKey)
                .addEncodedQueryParameter("redirect_uri", server.callbackUri.toString())
                .addQueryParameter("response_type", "code")
                .addEncodedQueryParameter("scope", scopes.value)
                .build()
                .toUrl()
                .toURI()
            browserLauncher(browserUrl)
            awaitClose {
                server.stopServing()
            }
        }

        val authorizationCode = channel.receive()

        apiCall.token(authorizationCode, accessKey, secretKey, server.callbackUri)
    }
}
