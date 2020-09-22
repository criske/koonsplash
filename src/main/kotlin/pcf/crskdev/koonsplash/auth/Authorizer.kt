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
import java.util.concurrent.Executor

/**
 * Authorizer for authenticated API requests.
 *
 * @property accessKey Access key.
 * @property secretKey Secret key.
 * @property server Authorize code callback server.
 * @property authCalls Authorization flow calls abstraction.
 * @property storage AuthToken storage.
 */
class Authorizer(
    private val accessKey: AccessKey,
    private val secretKey: SecretKey,
    private val server: AuthCodeServer = AuthCodeServerImpl(URI.create("http://localhost:3000")),
    private val authCalls: AuthCalls = AuthCallsImpl(HttpClient.http, JsonClient.json),
    private val storage: AuthTokenStorage,
) {

    /**
     * OAuth2 authorization flow done in background thread provided by executor.
     * unless there is an AuthToken saved in storage.
     *
     * @param executor Background thread.
     * @param loginFormController Login controller activates the login form if needed.
     * @param onError Failed authorizing callback.
     * @param onSuccess Success authorizing callback.
     * @receiver onError receives the error message.
     * @receiver onSuccess receives the AuthToken.
     */
    fun authorize(
        executor: Executor,
        loginFormController: LoginFormController,
        onError: (String) -> Unit,
        onSuccess: (AuthToken) -> Unit
    ) {
        val authToken = storage.load()
        if (authToken != null) {
            // token already saved, is safe to return
            onSuccess(authToken)
            return
        }

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
        executor.execute {
            authCalls
                .authorize(accessKey, server.callbackUri, AuthScope.PUBLIC)
                .onSuccess { code ->
                    authCalls.token(code, accessKey, secretKey, server.callbackUri)
                        .onSuccess(onSuccessAndClose)
                        .onFailure { onErrorAndClose(it.message ?: "Unknown error: $it") }
                }.onFailure { authorizeErr ->
                    if (authorizeErr is NeedsLogin) {
                        val loginFormSubmitter = object : LoginFormSubmitter {
                            override fun submit(email: String, password: String) {
                                executor.execute {
                                    authCalls.loginForm(authorizeErr.authenticityToken, email, password)
                                        .onSuccess { code ->
                                            authCalls.token(code, accessKey, secretKey, server.callbackUri)
                                                .onSuccess { token ->
                                                    loginFormController.onLoginSuccess()
                                                    storage.save(token)
                                                    onSuccessAndClose(token)
                                                }
                                                .onFailure { tokenErr ->
                                                    onErrorAndClose(tokenErr.message ?: "Unknown error: $tokenErr")
                                                }
                                        }
                                        .onFailure { loginErr ->
                                            if (loginErr is InvalidCredentials) {
                                                loginFormController.onLoginFailure()
                                                loginFormController.activateForm()
                                            } else {
                                                onErrorAndClose(loginErr.message ?: "Unknown error: $loginErr")
                                            }
                                        }
                                }
                            }
                        }
                        loginFormController.attachFormSubmitter(loginFormSubmitter)
                        loginFormController.activateForm()
                    } else {
                        onErrorAndClose(authorizeErr.message ?: "Unknown error: $authorizeErr")
                    }
                }
        }
    }
}

typealias AccessKey = String
typealias SecretKey = String
