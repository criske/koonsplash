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

package pcf.crskdev.koonsplash.api

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import pcf.crskdev.koonsplash.auth.AuthContext
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import pcf.crskdev.koonsplash.util.StringSpecIT
import pcf.crskdev.koonsplash.util.setBodyFromResource

internal class ApiAuthImplTest : StringSpecIT({

    val authContext = mockk<AuthContext>(relaxed = true)
    coEvery { authContext.getToken() } returns AuthToken("token_123", "bearer", "", AuthScope.ALL, 1)
    every { authContext.accessKey } returns "key_123"
    val api = ApiAuthImpl(
        mockk(),
        HttpClient.http,
        KoonsplashContext.Builder().auth { authContext }.build()
    )

    "should call me" {
        dispatchable = {
            when (path ?: "/") {
                "/me" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBodyFromResource("me.json")
                else -> MockResponse().setResponseCode(404)
            }
        }

        val me = api.me()

        me["id"]<String>() shouldBe "fakeid"
        me["username"]<String>() shouldBe "foo"

        with(lastRequest()) {
            method shouldBe "GET"
            headers.toMultimap()["Authorization"]!![0] shouldBe "Client-ID key_123"
            headers.toMultimap()["Authorization"]!![1] shouldBe "Bearer token_123"
        }
    }
})
