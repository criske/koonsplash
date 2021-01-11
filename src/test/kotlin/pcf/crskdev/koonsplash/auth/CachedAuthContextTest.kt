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

package pcf.crskdev.koonsplash.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

internal class CachedAuthContextTest : StringSpec({

    val dispatcherA = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    val dispatcherB = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    "should throw if auth storage is empty" {

        val storage = mockk<AuthTokenStorage>(relaxed = true)
        every { storage.load() } returns null

        val context = CachedAuthContext(storage, "")

        shouldThrow<SignedOutException> {
            context.getToken()
        }
    }

    "should throw if token is not set" {

        val storage = mockk<AuthTokenStorage>(relaxed = true)
        every { storage.load() } returns null

        val context = CachedAuthContext(storage, "")
        context.reset(AuthToken("", "", "", AuthScope.PUBLIC, 0))

        context.getToken().shouldNotBeNull()

        context.clear()
        shouldThrow<SignedOutException> {
            context.getToken()
        }
    }

    "should get token if it is already set in storage" {

        val storage = mockk<AuthTokenStorage>(relaxed = true)
        every { storage.load() } returns AuthToken("", "", "", AuthScope.PUBLIC, 0)

        val context = CachedAuthContext(storage, "")

        context.getToken().shouldNotBeNull()
    }

    "should ensure that token is set" {
        val storage = mockk<AuthTokenStorage>(relaxed = true)
        every { storage.load() } returns null

        val context = CachedAuthContext(storage, "")

        launch(Dispatchers.Default) {
            repeat(10) { count ->
                val isEven = (count + 1).rem(2) == 0
                val dispatcher = if (isEven) dispatcherA else dispatcherB
                launch(dispatcher) {
                    if (isEven) {
                        context.reset(AuthToken((count + 1).toString(), "", "", AuthScope.PUBLIC, 0))
                    } else {
                        context.clear()
                    }
                }.join()
            }
        }.join()

        val token = context.getToken()
        context.hasToken() shouldBe true
        token.shouldNotBeNull()
        token.accessToken shouldBe "10"
    }

    "has access key" {
        val context = CachedAuthContext(mockk(relaxed = true), "123")
        context.accessKey shouldBe "123"
    }
})
