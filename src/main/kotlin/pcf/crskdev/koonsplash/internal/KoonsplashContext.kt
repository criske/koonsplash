/*
 *  Copyright (c) 2021. Pela Cristian
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

import pcf.crskdev.koonsplash.api.ApiCall
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthContext
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.BrowserLauncher
import java.net.URI

/**
 * Koonsplash context.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal interface KoonsplashContext {

    /**
     * AuthContext.
     *
     */
    val auth: AuthContext

    /**
     * Open an Uri.
     *
     */
    val browserLauncher: BrowserLauncher

    /**
     * Provider for [ApiCall].
     */
    val apiCaller: (String) -> ApiCall

    /**
     * Builder for [KoonsplashContext].
     *
     * @param from Base upon.
     * @constructor Create empty Builder
     */
    class Builder(private val from: KoonsplashContext? = null) {

        /**
         * Auth
         */
        private var auth: AuthContext = from?.auth ?: object : AuthContext {
            override val accessKey: AccessKey
                get() = throw IllegalStateException("AuthContext not set")

            override suspend fun getToken(): AuthToken? =
                throw IllegalStateException("AuthContext not set")
        }

        /**
         * Browser launcher
         */
        private var browserLauncher: BrowserLauncher = from?.browserLauncher ?: object : BrowserLauncher {
            override fun launch(url: URI): Result<Unit> {
                throw IllegalStateException("BrowserLauncher not set")
            }
        }

        /**
         * Api caller
         */
        private var apiCaller: (String) -> ApiCall = from?.apiCaller ?: {
            throw IllegalStateException("Api caller not set")
        }

        /**
         * Set auth context.
         *
         * @param block
         * @receiver
         * @return Builder.
         */
        inline fun auth(block: (AuthContext) -> AuthContext): Builder {
            this.auth = block(this.auth)
            return this
        }

        /**
         * Browser launcher.
         *
         * @param browserLauncher
         * @return
         */
        fun browserLauncher(browserLauncher: BrowserLauncher): Builder {
            this.browserLauncher = browserLauncher
            return this
        }

        /**
         * Api caller.
         *
         * @param apiCaller
         * @receiver
         * @return
         */
        fun apiCaller(apiCaller: (String) -> ApiCall): Builder {
            this.apiCaller = apiCaller
            return this
        }

        /**
         * Builds the KoonsplashContext.
         *
         * @return KoonsplashContext.
         */
        fun build(): KoonsplashContext {
            val auth = this.auth
            val browserLauncher = this.browserLauncher
            val apiCaller = this.apiCaller
            return object : KoonsplashContext {
                override val auth: AuthContext
                    get() = auth
                override val browserLauncher: BrowserLauncher
                    get() = browserLauncher
                override val apiCaller: (String) -> ApiCall
                    get() = apiCaller
            }
        }
    }
}

/**
 * New builder based upon current context.
 * @return KoonsplashContext.Builder
 */
internal fun KoonsplashContext.newBuilder(): KoonsplashContext.Builder = KoonsplashContext.Builder(this)
