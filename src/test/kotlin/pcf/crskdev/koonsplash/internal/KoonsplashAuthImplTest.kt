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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pcf.crskdev.koonsplash.Koonsplash
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.CachedAuthContext
import pcf.crskdev.koonsplash.auth.SignedOutException
import pcf.crskdev.koonsplash.http.HttpClient

@ExperimentalCoroutinesApi
internal class KoonsplashAuthImplTest : StringSpec({

    "should not allow calling api after sign out" {
        val storage = mockk<AuthTokenStorage>(relaxed = true)
        val public = mockk<Koonsplash>()
        every { public.api } returns (mockk())

        val koonsplash = KoonsplashAuthImpl(
            public,
            CachedAuthContext(storage, ""),
            HttpClient.http
        )

        val backToPublic = koonsplash.signOut()
        verify { storage.clear() }
        backToPublic shouldBeSameInstanceAs public
        shouldThrow<SignedOutException> {
            koonsplash.api.me()
        }
    }
})
