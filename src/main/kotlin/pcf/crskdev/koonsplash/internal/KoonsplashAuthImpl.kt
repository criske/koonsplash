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

import kotlinx.coroutines.coroutineScope
import okhttp3.OkHttpClient
import pcf.crskdev.koonsplash.Koonsplash
import pcf.crskdev.koonsplash.api.ApiAuth
import pcf.crskdev.koonsplash.api.ApiAuthImpl
import pcf.crskdev.koonsplash.auth.safe

/**
 * Koonsplash auth implementation.
 */
internal class KoonsplashAuthImpl(
    private val public: Koonsplash,
    private val context: KoonsplashContext,
    private val httpClient: OkHttpClient,
) : Koonsplash.Auth {

    override val api: ApiAuth
        get() = ApiAuthImpl(
            public.api,
            httpClient,
            context
                .newBuilder()
                .auth { it.safe() }
                .build()
        )

    override suspend fun signOut(): Koonsplash = coroutineScope {
        context.auth.asClearable().clear()
        public
    }
}
