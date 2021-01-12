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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import java.lang.IllegalStateException
import java.net.URI

@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
internal class AuthCodeServerImplTest : StringSpec({

    "should receive code" {
        val code = withTimeout(10000) {
            produce {
                val server = AuthCodeServerImpl().apply {
                    startServing()
                    onAuthorizeCode {
                        offer(it)
                        close()
                    }
                }
                HttpClient.http.newCall(
                    Request.Builder().url(
                        server.callbackUri.toHttpUrlOrNull()!!.newBuilder()
                            .addQueryParameter("code", "123")
                            .build()
                    ).build()
                ).executeCo()
                awaitClose {
                    server.stopServing()
                }
            }.receive()
        }

        code shouldBe "123"
    }

    "should have a default port assigned" {
        val server = AuthCodeServerImpl(URI.create("http://localhost/"))
        server.startServing()
        server.callbackUri shouldBe URI.create("http://localhost:3000/")
        server.listeningPort shouldBe 3000
        server.stopServing()
    }

    "should override port from url with the one from parameters" {
        val server = AuthCodeServerImpl(URI.create("http://localhost:3000/"), 3001u)
        server.startServing()
        server.callbackUri shouldBe URI.create("http://localhost:3001/")
        server.listeningPort shouldBe 3001
        server.stopServing()
    }

    "should throw if callback is invalid" {
        val server = AuthCodeServerImpl(URI.create("/"), 3001u)
        shouldThrow<IllegalStateException> {
            server.callbackUri
        }
    }

    "should have a default URI" {
        val server = AuthCodeServerImpl()
        server.startServing()
        server.callbackUri shouldBe URI.create("http://localhost:3000/")
        server.listeningPort shouldBe 3000
        server.stopServing()
    }
})
