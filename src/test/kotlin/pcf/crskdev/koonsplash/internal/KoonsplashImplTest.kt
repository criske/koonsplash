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
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import pcf.crskdev.koonsplash.api.ApiImpl
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.Authorizer
import pcf.crskdev.koonsplash.auth.CachedAuthContext
import pcf.crskdev.koonsplash.auth.SecretKey
import pcf.crskdev.koonsplash.http.HttpClient
import java.net.URI

@ExperimentalStdlibApi
internal class KoonsplashImplTest : StringSpec({

    "should return api" {
        val koonsplash = KoonsplashImpl(
            "key_123",
            CachedAuthContext(mockk(relaxed = true), "access_123"),
            HttpClient.http,
            mockk()
        )

        koonsplash.api.shouldBeInstanceOf<ApiImpl>()
    }

    "should not call authorize if authToken is stored" {
        val storage = mockk<AuthTokenStorage>(relaxed = true)
        val authorizer = mockk<Authorizer>(relaxed = true)
        val koonsplash = KoonsplashImpl(
            "key_123",
            CachedAuthContext(storage, ""),
            HttpClient.http,
            authorizer
        )

        every { storage.load() } returns AuthToken("token", "", "", AuthScope.ALL, 1)

        val authenticated = koonsplash.authenticated {}

        // TODO fix this
        // coVerify (exactly = 0) { authorizer.authorize(any(), any(), any(), any(), any()) }
        authenticated.shouldBeInstanceOf<KoonsplashAuthImpl>()
    }

    "should call authorize" {
        val storage = mockk<AuthTokenStorage>(relaxed = true)
        val authToken = AuthToken("token", "", "", AuthScope.ALL, 1)
        val authorizer = object : Authorizer {
            override suspend fun authorize(
                accessKey: AccessKey,
                secretKey: SecretKey,
                scopes: AuthScope,
                browserLauncher: (URI) -> Unit
            ): AuthToken {
                return authToken
            }
        }
        every { storage.load() } returns null
        val koonsplash = KoonsplashImpl(
            "key_123",
            CachedAuthContext(storage, ""),
            HttpClient.http,
            authorizer
        )

        val authenticated = koonsplash.authenticated {}

        coVerify(exactly = 1) { storage.save(authToken) }
        authenticated.shouldBeInstanceOf<KoonsplashAuthImpl>()
    }

    "should throw error authorization fail" {
        val storage = mockk<AuthTokenStorage>(relaxed = true)
        val authorizer = object : Authorizer {
            override suspend fun authorize(
                accessKey: AccessKey,
                secretKey: SecretKey,
                scopes: AuthScope,
                browserLauncher: (URI) -> Unit
            ): AuthToken {
                throw IllegalStateException("Failed to authorize")
            }
        }
        every { storage.load() } returns null
        val koonsplash = KoonsplashImpl(
            "key_123",
            CachedAuthContext(storage, ""),
            HttpClient.http,
            authorizer
        )

        shouldThrow<IllegalStateException> {
            koonsplash.authenticated {}
        }
    }
})
