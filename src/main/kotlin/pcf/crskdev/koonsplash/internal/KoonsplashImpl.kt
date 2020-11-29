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

package pcf.crskdev.koonsplash.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import pcf.crskdev.koonsplash.Koonsplash
import pcf.crskdev.koonsplash.api.Api
import pcf.crskdev.koonsplash.api.ApiImpl
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthCodeServerImpl
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.Authorizer
import pcf.crskdev.koonsplash.auth.LoginFormController
import pcf.crskdev.koonsplash.auth.SecretKey
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 *  Koonsplash entry point implementation.
 *
 * @property accessKey AccessKey.
 * @property secretKey SecretKey.
 * @property storage Storage.
 * @property httpClient OkHttpClient.
 * @property authorizer Authorizer.
 * @constructor Create empty Koonsplash impl
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalStdlibApi
class KoonsplashImpl(
    private val accessKey: AccessKey,
    private val secretKey: SecretKey,
    private val storage: AuthTokenStorage,
    private val httpClient: OkHttpClient,
    private val authorizer: Authorizer
) : Koonsplash {

    override val api: Api = ApiImpl(httpClient, accessKey)

    override suspend fun authenticated(controller: LoginFormController, scopes: AuthScope): Koonsplash.Auth =
        coroutineScope {
            val authToken = storage.load()
            if (authToken == null) {
                val dispatcher = coroutineContext[CoroutineDispatcher]
                requireNotNull(dispatcher)
                val executor = dispatcher.asExecutor()
                suspendCoroutine { cont ->
                    authorizer.authorize(
                        executor,
                        controller,
                        scopes,
                        { cont.resumeWithException(it) },
                        {
                            storage.save(it)
                            cont.resume(
                                KoonsplashAuthImpl(it, accessKey, this@KoonsplashImpl, storage, httpClient)
                            )
                        }
                    )
                }
            } else {
                KoonsplashAuthImpl(authToken, accessKey, this@KoonsplashImpl, storage, httpClient)
            }
        }

    override suspend fun authenticated(scopes: AuthScope, port: Int, browserLauncher: (URI) -> Unit): Koonsplash.Auth {
        val authToken = storage.load()
        val authTokenEnsured = if (authToken == null) {
            val newAuthToken = authorizer.authorize(
                this.accessKey,
                this.secretKey,
                scopes,
                AuthCodeServerImpl(URI.create("http://localhost:$port")),
                browserLauncher
            )
            storage.save(newAuthToken)
            newAuthToken
        } else {
            authToken
        }
        return KoonsplashAuthImpl(authTokenEnsured, accessKey, this@KoonsplashImpl, storage, httpClient)
    }
}
