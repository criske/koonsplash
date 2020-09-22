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

import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.json.JsonClient
import java.net.URI

class Authorizer(
    private val accessKey: AccessKey,
    private val secretKey: SecretKey,
    private val server: AuthCodeServer = AuthCodeServerImpl(URI.create("http://localhost:3000")),
    private val authCalls: AuthCalls = AuthCallsImpl(HttpClient.http, JsonClient.json),
    private val storage: AuthTokenStorage,
) {

    fun authorize(
        email: String,
        password: String,
        onError: (String) -> Unit,
        onSuccess: (AuthToken) -> Unit
    ) {
        val hasStarted = server.startServing()
        if (!hasStarted) {
            onError("Auth code server hasn't started")
        }
        val onSuccessAndClose: (AuthToken) -> Unit = {
            server.stopServing()
            onSuccess(it)
        }
        val onErrorAndClose: (String) -> Unit = {
            server.stopServing()
            onError(it)
        }
        val authorize = authCalls.authorize(accessKey, server.callbackUri, AuthScope.PUBLIC)
        authorize.onSuccess {
            authCalls.token(it, accessKey, secretKey, server.callbackUri)
                .onSuccess(onSuccessAndClose)
                .onFailure {
                    onErrorAndClose(it.message ?: "Unknown error: $it")
                }
        }.onFailure {
            if (it is NeedsLogin) {
                authCalls.loginForm(it.authenticityToken, email, password)
                    .onSuccess {
                        authCalls.token(it, accessKey, secretKey, server.callbackUri)
                            .onSuccess(onSuccessAndClose)
                            .onFailure {
                                onErrorAndClose(it.message ?: "Unknown error: $it")
                            }
                    }
                    .onFailure {
                        onErrorAndClose(it.message ?: "Unknown error: $it")
                    }
            } else {
                onErrorAndClose(it.message ?: "Unknown error: $it")
            }
        }
    }
}

typealias AccessKey = String
typealias SecretKey = String
