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

package pcf.crskdev.koonsplash

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import pcf.crskdev.koonsplash.api.Api
import pcf.crskdev.koonsplash.api.ApiAuth
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.ApiKeysLoader
import pcf.crskdev.koonsplash.auth.AuthApiCallImpl
import pcf.crskdev.koonsplash.auth.AuthCodeServer
import pcf.crskdev.koonsplash.auth.AuthCodeServerImpl
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.AuthorizerImpl
import pcf.crskdev.koonsplash.auth.CachedAuthContext
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashImpl
import pcf.crskdev.koonsplash.internal.KoonsplashSingleton
import java.net.URI

/**
 * Koonsplash entry point.
 *
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalUnsignedTypes
interface Koonsplash : KoonsplashEntry {

    /**
     * Api
     */
    val api: Api

    /**
     * Authenticated session.
     *
     * @param scopes Scopes
     * @param port Port that code server will listen.
     * @param host: AuthCode server host
     * @param port: AuthCode server port
     * @param browserLauncher Browser launcher.
     * @receiver contains URI to launch the OS browser.
     * @return Authenticated session.
     */
    suspend fun authenticated(
        scopes: AuthScope = AuthScope.ALL,
        host: String = AuthCodeServer.DEFAULT_HOST,
        port: UInt = AuthCodeServer.DEFAULT_PORT,
        browserLauncher: (URI) -> Unit
    ): Auth

    /**
     * Koonsplash entry point for authenticated requests.
     *
     */
    interface Auth : KoonsplashEntry {

        val api: ApiAuth

        suspend fun signOut(): Koonsplash
    }

    @ExperimentalStdlibApi
    companion object {
        /**
         * Helper to create a new Koonsplash object.
         *
         * @param accessKey Access Key (client id)
         * @return Koonsplash
         */
        fun builder(accessKey: AccessKey): KoonsplashBuilder =
            KoonsplashBuilder(accessKey)
    }
}

/**
 * Flag that marks entries for this lib.
 *
 * @constructor Create empty Flag
 */
interface KoonsplashEntry

/**
 * Koonsplash builder.
 *
 * @property accessKey: AccessKey(client id)
 */
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class KoonsplashBuilder internal constructor(
    private val accessKey: AccessKey,
) {

    /**
     * Default Dispatcher
     */
    private var dispatcher = Dispatchers.IO


    //private var authTokenStorage: AuthTokenStorage =

    /**
     * Dispatcher.
     *
     * @param dispatcher CoroutineDispatcher
     */
    fun dispatcher(dispatcher: CoroutineDispatcher): KoonsplashBuilder {
        this.dispatcher = dispatcher
        return this
    }

    fun authTokenStorage(authTokenStorage: AuthTokenStorage): KoonsplashBuilder {

    }

    fun build(): Koonsplash {
        val authorizer = AuthorizerImpl(AuthApiCallImpl()) { host, port ->
            AuthCodeServerImpl(AuthCodeServer.createCallback(host, port))
        }
        val authContext = CachedAuthContext(this.storage, this.keysLoader.accessKey)
        return KoonsplashSingleton(
            KoonsplashImpl(
                this.keysLoader.secretKey,
                authContext,
                HttpClient.http,
                authorizer
            )
        )
    }
}
