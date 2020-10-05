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
import kotlinx.coroutines.coroutineScope
import pcf.crskdev.koonsplash.api.Api
import pcf.crskdev.koonsplash.api.ApiAuth
import pcf.crskdev.koonsplash.auth.ApiKeysLoader
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.AuthorizerImpl
import pcf.crskdev.koonsplash.auth.LoginFormController
import pcf.crskdev.koonsplash.auth.OneShotLoginFormController
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashImpl

/**
 * Koonsplash entry point.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface Koonsplash {

    /**
     * Api
     */
    val api: Api

    /**
     * Tries to authenticate by interacting with a [LoginFormController].
     *
     * @param controller Controller
     * @return [Koonsplash.Auth]
     */
    suspend fun authenticated(controller: LoginFormController, scopes: AuthScope = AuthScope.ALL): Auth

    /**
     * One shot authentication.
     *
     * @param email Email
     * @param password Password
     * @return [Koonsplash.Auth]
     */
    suspend fun authenticated(email: String, password: String, scopes: AuthScope = AuthScope.ALL): Auth = coroutineScope {
        authenticated(OneShotLoginFormController(email, password), scopes)
    }

    /**
     * Koonsplash entry point for authenticated requests.
     *
     */
    interface Auth {

        val api: ApiAuth

        suspend fun signOut(): Koonsplash
    }

    @ExperimentalStdlibApi
    companion object {
        /**
         * Helper to create a new Koonsplash object.
         *
         * @param keysLoader ApiKeysLoader
         * @param storage AuthTokenStorage
         * @return Koonsplash
         */
        fun builder(keysLoader: ApiKeysLoader, storage: AuthTokenStorage): KoonsplashBuilder =
            KoonsplashBuilder(keysLoader, storage)
    }
}

/**
 * Koonsplash builder.
 *
 * @property keysLoader ApiKeysLoader
 * @property storage AuthTokenStorage
 */
@ExperimentalStdlibApi
class KoonsplashBuilder internal constructor(
    private val keysLoader: ApiKeysLoader,
    private val storage: AuthTokenStorage
) {

    /**
     * Default Dispatcher
     */
    private var dispatcher = Dispatchers.IO

    /**
     * Dispatcher.
     *
     * @param dispatcher CoroutineDispatcher
     */
    fun dispatcher(dispatcher: CoroutineDispatcher): KoonsplashBuilder {
        this.dispatcher = dispatcher
        return this
    }

    fun build(): Koonsplash {
        val authorizer = AuthorizerImpl(this.keysLoader.accessKey, this.keysLoader.secretKey)
        return KoonsplashImpl(this.keysLoader.accessKey, this.storage, HttpClient.http, authorizer)
    }
}
