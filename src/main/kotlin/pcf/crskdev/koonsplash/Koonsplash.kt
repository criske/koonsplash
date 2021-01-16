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
import pcf.crskdev.koonsplash.auth.AuthApiCallImpl
import pcf.crskdev.koonsplash.auth.AuthCodeServer
import pcf.crskdev.koonsplash.auth.AuthCodeServerImpl
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.AuthorizerImpl
import pcf.crskdev.koonsplash.auth.BrowserLauncherImpl
import pcf.crskdev.koonsplash.auth.CachedAuthContext
import pcf.crskdev.koonsplash.auth.SecretKey
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import pcf.crskdev.koonsplash.internal.KoonsplashImpl
import pcf.crskdev.koonsplash.internal.KoonsplashSingleton
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * Koonsplash entry point.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface Koonsplash : KoonsplashEntry {

    /**
     * Api
     */
    val api: Api

    /**
     * Authenticated session.
     *
     * @param builder AuthenticatedBuilder.
     * @return Authenticated session.
     */
    suspend fun authenticated(builder: AuthenticatedBuilder): Auth

    /**
     * Koonsplash entry point for authenticated requests.
     *
     */
    interface Auth : KoonsplashEntry {

        val api: ApiAuth

        suspend fun signOut(): Koonsplash
    }

    companion object {
        /**
         * Helper to create a new Koonsplash object.
         *
         * @param accessKey Access Key (client id)
         * @return Koonsplash
         */
        @ExperimentalUnsignedTypes
        @ExperimentalTime
        @ExperimentalStdlibApi
        fun builder(accessKey: AccessKey): KoonsplashBuilder =
            KoonsplashBuilder(accessKey)
    }

    /**
     * Authenticated builder
     *
     * @property secretKey Secret key.
     */
    class AuthenticatedBuilder(internal val secretKey: SecretKey) {

        /**
         * Scopes.
         */
        private var scopes: AuthScope = AuthScope.ALL

        /**
         * Host.
         */
        private var host: String = AuthCodeServer.DEFAULT_HOST

        /**
         * Port.
         */
        @ExperimentalUnsignedTypes
        private var port: UInt = AuthCodeServer.DEFAULT_PORT

        /**
         * Timeout.
         */
        @ExperimentalTime
        private var timeout: Duration = 5.toDuration(TimeUnit.MINUTES)

        /**
         * Browser launcher.
         */
        private var browserLauncher: ((URI) -> Unit)? = null

        /**
         * Scopes.
         *
         * @param scopes
         * @return AuthenticatedBuilder
         */
        fun scopes(scopes: AuthScope): AuthenticatedBuilder {
            this.scopes = scopes
            return this
        }

        /**
         * Host of the authorization code server (should be localhost or a local ip)
         *
         * @param host
         * @return AuthenticatedBuilder
         */
        fun host(host: String): AuthenticatedBuilder {
            this.host = host
            return this
        }

        /**
         * Port that authorization code server will listen.
         *
         * @param port
         * @return AuthenticatedBuilder
         */
        @ExperimentalUnsignedTypes
        fun port(port: UInt): AuthenticatedBuilder {
            this.port = port
            return this
        }

        /**
         * Amount of time within the authorization must execute. Default 5 minutes.
         *
         * @param timeout
         * @return AuthenticatedBuilder
         */
        @ExperimentalTime
        fun timeout(timeout: Duration): AuthenticatedBuilder {
            this.timeout = timeout
            return this
        }

        /**
         * Launches the os browser app.
         *
         * It should be used mainly on android. If null it will try to use the internal browser launcher.
         *
         * @param browserLauncher
         * @receiver Contains URI to launch the OS browser.
         * @return AuthenticatedBuilder
         */
        fun browserLauncher(browserLauncher: (URI) -> Unit): AuthenticatedBuilder {
            this.browserLauncher = browserLauncher
            return this
        }

        /**
         * Build a new Authenticated object.
         *
         * @return Authenticated.
         */
        @ExperimentalUnsignedTypes
        @ExperimentalTime
        internal fun build(): Authenticated = Authenticated(
            this.secretKey,
            this.scopes,
            this.host,
            this.port,
            this.timeout,
            this.browserLauncher
        )

        @ExperimentalUnsignedTypes
        @ExperimentalTime
        internal class Authenticated(
            internal val secretKey: SecretKey,
            internal val scopes: AuthScope = AuthScope.ALL,
            internal val host: String = AuthCodeServer.DEFAULT_HOST,
            internal val port: UInt = AuthCodeServer.DEFAULT_PORT,
            internal val timeout: Duration = 5.toDuration(TimeUnit.MINUTES),
            internal val browserLauncher: ((URI) -> Unit)? = null
        )
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
@ExperimentalTime
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
class KoonsplashBuilder internal constructor(
    private val accessKey: AccessKey,
) {

    /**
     * Default Dispatcher
     */
    private var dispatcher = Dispatchers.IO

    /**
     * Default auth token storage.
     */
    private var authTokenStorage: AuthTokenStorage = AuthTokenStorage.None

    /**
     * Open links strategy.
     */
    private var openLinksStrategy: ((URI) -> Result<Unit>)? = null

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
        this.authTokenStorage = authTokenStorage
        return this
    }

    /**
     * Open Links Strategy.
     * If there is no strategy, it will try to launch the url using the OS default browser.
     *
     * Note: this mainly will work if OS is a desktop one (win, linux, mac).
     * On mobile (android), user should handle the launch intent themselves by providing the `openLinksStrategy`.
     *
     * @param strategy
     * @receiver Provide logic on how to open the URI. Returns Result.success(Unit) if all is ok
     * return
     */
    fun openLinksStrategy(strategy: (URI) -> Result<Unit>): KoonsplashBuilder {
        this.openLinksStrategy = strategy
        return this
    }

    fun build(): Koonsplash {
        val authContext = CachedAuthContext(this.authTokenStorage, this.accessKey)
        val browserLauncher = BrowserLauncherImpl(externalLauncher = this.openLinksStrategy)
        val authorizer = AuthorizerImpl(AuthApiCallImpl(), browserLauncher) { host, port ->
            AuthCodeServerImpl(AuthCodeServer.createCallback(host, port))
        }
        val koonsplashContext = KoonsplashContext.Builder()
            .auth { authContext }
            .browserLauncher(browserLauncher)
            .build()
        return KoonsplashSingleton(
            KoonsplashImpl(
                koonsplashContext,
                HttpClient.http,
                authorizer
            )
        )
    }
}
