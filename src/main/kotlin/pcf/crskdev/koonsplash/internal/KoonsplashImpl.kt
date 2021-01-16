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

import okhttp3.OkHttpClient
import pcf.crskdev.koonsplash.Koonsplash
import pcf.crskdev.koonsplash.api.Api
import pcf.crskdev.koonsplash.api.ApiImpl
import pcf.crskdev.koonsplash.auth.AuthContext
import pcf.crskdev.koonsplash.auth.Authorizer
import java.util.Arrays
import kotlin.time.ExperimentalTime

/**
 *  Koonsplash entry point implementation.
 *
 * @property httpClient OkHttpClient.
 * @property authorizer Authorizer.
 * @constructor Create empty Koonsplash impl
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalTime
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
internal class KoonsplashImpl(
    private val koonsplashContext: KoonsplashContext,
    private val httpClient: OkHttpClient,
    private val authorizer: Authorizer
) : Koonsplash {

    override val api: Api = ApiImpl(
        httpClient,
        koonsplashContext
            .newBuilder()
            .auth { AuthContext.None(it.accessKey) }
            .build()
    )

    override suspend fun authenticated(builder: Koonsplash.AuthenticatedBuilder): Koonsplash.Auth {
        try {
            val clearableAuthContext = koonsplashContext.auth.asClearable()
            if (!clearableAuthContext.hasToken()) {
                val authenticated = builder.build()
                val newAuthToken = authorizer.authorize(
                    clearableAuthContext.accessKey,
                    authenticated.secretKey,
                    authenticated.scopes,
                    authenticated.host,
                    authenticated.port,
                    authenticated.timeout
                )
                clearableAuthContext.reset(newAuthToken)
            }
        } finally {
            Arrays.fill(builder.secretKey, ' ')
        }
        return KoonsplashAuthImpl(this, this.koonsplashContext, httpClient)
    }
}
