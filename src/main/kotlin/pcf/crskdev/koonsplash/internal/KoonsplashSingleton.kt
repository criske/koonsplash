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

import pcf.crskdev.koonsplash.Koonsplash
import pcf.crskdev.koonsplash.KoonsplashEntry
import pcf.crskdev.koonsplash.auth.AuthScope
import java.net.URI

/**
 * Singleton Wrapper for [Koonsplash] lib entries.
 *
 * @property delegate
 * @constructor Create empty Singleton
 */
@ExperimentalUnsignedTypes
class KoonsplashSingleton internal constructor(private val delegate: Koonsplash) : Koonsplash by delegate {

    companion object {
        /**
         * Instance internal. Visible for tests.
         */
        @Volatile
        internal var instanceInternal: KoonsplashEntry? = null

        /**
         * Instance that returns Koonsplash medium, authenticated or not
         */
        val instance: KoonsplashEntry
            get() =
                instanceInternal ?: throw IllegalStateException("Koonsplash not initialized; use Builder first")
    }

    init {
        instanceInternal = this
    }

    override suspend fun authenticated(
        scopes: AuthScope,
        host: String,
        port: UInt,
        browserLauncher: (URI) -> Unit
    ): Koonsplash.Auth {

        if (instanceInternal is Koonsplash.Auth) {
            throw IllegalStateException("Already authenticated. Sign out first")
        }

        return KoonsplashSingleton.Auth(delegate.authenticated(scopes, host, port, browserLauncher)).apply {
            instanceInternal = this
        }
    }

    class Auth(private val delegate: Koonsplash.Auth) : Koonsplash.Auth by delegate {

        override suspend fun signOut(): Koonsplash = delegate.signOut().apply {
            instanceInternal = this
        }
    }
}
