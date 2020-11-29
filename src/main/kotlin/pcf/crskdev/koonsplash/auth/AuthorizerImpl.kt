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
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import pcf.crskdev.koonsplash.http.HttpClient.jsonBody
import java.net.URI
import java.util.concurrent.Executor

/**
 * Authorizer implementation for authenticated API requests.
 *
 * @property accessKey Access key.
 * @property secretKey Secret key.
 * @property server Authorize code callback server.
 * @property authCalls Authorization flow calls abstraction.
 */
internal class AuthorizerImpl(
    private val accessKey: AccessKey,
    private val secretKey: SecretKey,
    private val server: AuthCodeServer = AuthCodeServerImpl(URI.create("http://localhost:3000")),
    private val authCalls: AuthCalls = AuthCallsImpl(HttpClient.http)
) : Authorizer {

    /**
     * OAuth2 authorization flow done in background thread provided by executor.
     * unless there is an AuthToken saved in storage.
     *
     * @param executor Background thread.
     * @param loginFormController Login controller activates the login form if needed.
     * @param onError Failed authorizing callback.
     * @param onSuccess Success authorizing callback.
     * @param scopes Auth Scopes
     * @receiver onError receives the error message.
     * @receiver onSuccess receives the AuthToken.
     */
    override fun authorize(
        executor: Executor,
        loginFormController: LoginFormController,
        scopes: AuthScope,
        onError: (Throwable) -> Unit,
        onSuccess: (AuthToken) -> Unit
    ) {
        val hasStarted = server.startServing()
        if (!hasStarted) {
            onError(IllegalStateException("Auth code server hasn't started"))
        }
        val onSuccessAndClose: (AuthToken) -> Unit = {
            server.stopServing()
            onSuccess(it)
        }
        val onErrorAndClose: (Throwable) -> Unit = {
            server.stopServing()
            onError(it)
        }
        executor.execute {
            authCalls
                .authorize(accessKey, server.callbackUri, scopes)
                .onSuccess { code ->
                    authCalls.token(code, accessKey, secretKey, server.callbackUri)
                        .onSuccess(onSuccessAndClose)
                        .onFailure { onErrorAndClose(it) }
                }.onFailure { authorizeErr ->
                    if (authorizeErr is NeedsLoginException) {
                        val loginFormSubmitter = object : LoginFormSubmitter {

                            override fun submit(email: String, password: String) {
                                executor.execute {
                                    authCalls.loginForm(authorizeErr.authenticityToken, email, password)
                                        .onSuccess { code ->
                                            authCalls.token(code, accessKey, secretKey, server.callbackUri)
                                                .onSuccess { token ->
                                                    onSuccessAndClose(token)
                                                    loginFormController.onLoginSuccess()
                                                    loginFormController.detachAll()
                                                }
                                                .onFailure { tokenErr ->
                                                    loginFormController.detachAll()
                                                    onErrorAndClose(tokenErr)
                                                }
                                        }
                                        .onFailure { loginErr ->
                                            when (loginErr) {
                                                InvalidCredentialsException -> {
                                                    loginFormController.onLoginFailure(loginErr)
                                                    loginFormController.activateForm(loginErr)
                                                }
                                                is NeedsConfirmAuthorizeFormException -> {
                                                    authCalls.authorizeForm(loginErr.form)
                                                        .onSuccess { code ->
                                                            authCalls.token(
                                                                code,
                                                                accessKey,
                                                                secretKey,
                                                                server.callbackUri
                                                            )
                                                                .onSuccess { token ->
                                                                    onSuccessAndClose(token)
                                                                    loginFormController.onLoginSuccess()
                                                                    loginFormController.detachAll()
                                                                }
                                                                .onFailure { tokenErr ->
                                                                    loginFormController.detachAll()
                                                                    onErrorAndClose(tokenErr)
                                                                }
                                                        }
                                                        .onFailure { formError ->
                                                            loginFormController.detachAll()
                                                            onErrorAndClose(formError)
                                                        }
                                                }
                                                else -> {
                                                    loginFormController.detachAll()
                                                    onErrorAndClose(loginErr)
                                                }
                                            }
                                        }
                                }
                            }

                            override fun giveUp(cause: Throwable?) {
                                onErrorAndClose(cause ?: GiveUpException)
                            }
                        }
                        loginFormController.attachFormSubmitter(loginFormSubmitter)
                        loginFormController.activateForm(null)
                    } else {
                        loginFormController.detachAll()
                        onErrorAndClose(authorizeErr)
                    }
                }
        }
    }

    @ExperimentalCoroutinesApi
    override suspend fun authorize(
        accessKey: AccessKey,
        secretKey: SecretKey,
        scopes: AuthScope,
        server: AuthCodeServer,
        browserLauncher: (URI) -> Unit
    ): AuthToken = coroutineScope {
        if (!server.startServing()) {
            throw IllegalStateException("Auth code server hasn't started")
        }
        val baseAuthHttpUrl = HttpClient.baseUrl.toHttpUrlOrNull()!!
            .newBuilder()
            .addPathSegment("oauth")
            .build()

        val browserUrl = baseAuthHttpUrl
            .newBuilder()
            .addPathSegment("authorize")
            .addQueryParameter("client_id", accessKey)
            .addEncodedQueryParameter("redirect_uri", server.callbackUri.toString())
            .addQueryParameter("response_type", "code")
            .addEncodedQueryParameter("scope", scopes.value)
            .build()
            .toUrl()
            .toURI()

        browserLauncher(browserUrl)

        val channel = produce(coroutineContext) {
            server.onAuthorizeCode {
                offer(it)
                close()
            }
            awaitClose {
                server.stopServing()
            }
        }

        val authorizationCode = channel.receive()

        val authTokenForm = FormBody.Builder()
            .add("client_id", accessKey)
            .add("client_secret", secretKey)
            .add("redirect_uri", server.callbackUri.toString())
            .add("code", authorizationCode)
            .add("grant_type", "authorization_code")
            .build()
        val request = Request.Builder()
            .url(baseAuthHttpUrl.newBuilder().addPathSegment("token").build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .post(authTokenForm)
            .build()

        HttpClient.http.newCall(request).executeCo().jsonBody()
    }
}
