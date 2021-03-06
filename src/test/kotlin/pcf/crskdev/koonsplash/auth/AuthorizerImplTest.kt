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

package pcf.crskdev.koonsplash.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.mockwebserver.MockResponse
import pcf.crskdev.koonsplash.util.server
import pcf.crskdev.koonsplash.util.withJSONHeader
import java.net.URI
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalStdlibApi
@ExperimentalTime
@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
internal class AuthorizerImplTest : StringSpec({

    "should authorize and get token" {
        server {
            beforeStart {
                dispatcher {
                    val path = it.path ?: ""
                    when {
                        path.contains("token") -> {
                            val token = """
                             {
                                "access_token": "091343ce13c8ae780065ecb3b13dc903475dd22cb78a05503c2e0c69c5e98044",
                                "token_type": "bearer",
                                "refresh_token": "",
                                "scope": "public read_photos write_photos",
                                "created_at": 1436544465
                             } 
                             """
                            MockResponse()
                                .withJSONHeader()
                                .setResponseCode(200)
                                .setBody(token)
                        }
                        else -> MockResponse().setResponseCode(400)
                    }
                }
            }
            val codeServer = MockAuthCodeServer()
            val browserLauncher = object : BrowserLauncher {
                override fun launch(url: URI): Result<Unit> {
                    codeServer.enqueueCode("123code")
                    return Result.success(Unit)
                }
            }
            val authorizer = AuthorizerImpl(AuthTokenCallImpl(), browserLauncher) { _, _ -> codeServer }

            val token = authorizer.authorize(
                "123",
                "123".toCharArray(),
                AuthScope.PUBLIC,
            )

            token.accessToken shouldBe "091343ce13c8ae780065ecb3b13dc903475dd22cb78a05503c2e0c69c5e98044"
            token.tokenType shouldBe "bearer"
            token.refreshToken shouldBe ""
            token.scope shouldBe AuthScope.PUBLIC + AuthScope.READ_PHOTOS + AuthScope.WRITE_PHOTOS
            token.createdAt shouldBe 1436544465
        }
    }

    "should fail launching the browser" {

        val codeServer = MockAuthCodeServer()
        val browserLauncher = object : BrowserLauncher {
            override fun launch(url: URI): Result<Unit> {
                return Result.failure(IllegalStateException("Failed to launch"))
            }
        }
        val authorizer = AuthorizerImpl(AuthTokenCallImpl(), browserLauncher) { _, _ -> codeServer }

        shouldThrow<IllegalStateException> {
            authorizer.authorize(
                "123",
                "123".toCharArray(),
                AuthScope.PUBLIC,
            )
        }
    }

    "should time out" {
        val codeServer = MockAuthCodeServer()
        val browserLauncher = object : BrowserLauncher {
            override fun launch(url: URI): Result<Unit> {
                return Result.success(Unit)
            }
        }
        val authorizer = AuthorizerImpl(AuthTokenCallImpl(), browserLauncher) { _, _ -> codeServer }

        shouldThrow<TimeoutCancellationException> {
            runBlockingTest {
                pauseDispatcher {
                    authorizer.authorize(
                        "123",
                        "123".toCharArray(),
                        AuthScope.PUBLIC,
                        timeout = 1.toDuration(DurationUnit.MINUTES)
                    )
                    advanceTimeBy(2.toDuration(DurationUnit.MINUTES).toLongMilliseconds())
                    runCurrent()
                }
            }
        }
    }
})
