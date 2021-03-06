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

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthContext
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import pcf.crskdev.koonsplash.util.StringSpecIT
import pcf.crskdev.koonsplash.util.resource
import pcf.crskdev.koonsplash.util.setBodyFromResource
import pcf.crskdev.koonsplash.util.toFile
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
internal class ApiCallITest : StringSpecIT({

    val api = ApiImpl(
        HttpClient.http,
        KoonsplashContext.Builder().auth { AuthContext.None("key_123") }.build()
    )

    val context = KoonsplashContext.Builder().auth {
        object : AuthContext {
            override val accessKey: AccessKey
                get() = "key_123"

            override suspend fun getToken(): AuthToken? =
                AuthToken("token_123", "bearer", "", AuthScope.ALL, 1L)
        }
    }.build()

    "should perform api call" {
        dispatchable = {
            when (path ?: "/") {
                "/photos/random/test?page=1" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBodyFromResource("random.json")
                else ->
                    MockResponse().setResponseCode(404)
            }
        }
        val apiCall = api.call("/photos/random/{test}?page={number}")
        val response = apiCall.invoke("test", 1)

        response["id"]<String>() shouldBe "fgc48MAG3Tk"
    }

    "should perform api call with raw progress" {
        dispatchable = {
            when (path ?: "/") {
                "/photos/random/test?page=1" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBodyFromResource("random.json")
                else ->
                    MockResponse().setResponseCode(404)
            }
        }
        val apiCall = api.call("/photos/random/{test}?page={number}")
        val statuses = apiCall
            .execute(listOf("test", 1), ApiCall.Progress.Raw)
            .toList()

        statuses.filterIsInstance<ApiCall.Status.Current<*>>()
            .toList()
            .map { it.value.toLong() }
            .maxOrNull() shouldBe resource("random.json").toFile().length()
    }

    "should perform api call with percent progress" {
        dispatchable = {
            when (path ?: "/") {
                "/photos/random/test?page=1" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBodyFromResource("random.json")
                        .throttleBody(500, 10, TimeUnit.MILLISECONDS)
                else ->
                    MockResponse().setResponseCode(404)
            }
        }

        val apiCall = api.call("/photos/random/{test}?page={number}")
        val statuses = apiCall
            .execute(listOf("test", 1), ApiCall.Progress.Percent)
            .toList()

        statuses.filterIsInstance<ApiCall.Status.Current<*>>()
            .toList()
            .map { it.value.toInt() }
            .maxOrNull() shouldBe 100
    }

    "should cancel api call" {
        dispatchable = {
            when (path ?: "/") {
                "/photos/random/test?page=1" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBodyFromResource("random.json")
                        .throttleBody(100, 1, TimeUnit.MINUTES)
                else ->
                    MockResponse().setResponseCode(404)
            }
        }

        val cancelSignal = MutableSharedFlow<Unit>()
        val apiCall = api.call("/photos/random/{test}?page={number}")
        launch {
            shouldThrowAny {
                apiCall("test", 1, cancel = cancelSignal)
            }
        }
        launch {
            delay(500)
            cancelSignal.emit(Unit)
        }.join()
    }

    "should post a resource" {
        dispatchable = {
            when (path ?: "/") {
                "/me" -> MockResponse().setResponseCode(204)
                else -> MockResponse().setResponseCode(404)
            }
        }

        ApiCallImpl(
            Endpoint(
                HttpClient.apiBaseUrl.toString(),
                "/me",
                Verb.Modify.Post(
                    "username" to "foo"
                )
            ),
            HttpClient.http,
            context
        )()

        with(lastRequest()) {
            method shouldBe "POST"
            headers.toMultimap()["Authorization"]!![1] shouldBe "Bearer token_123"
            body.toString() shouldBe "[text=username=foo]"
        }
    }

    "should put a resource" {
        dispatchable = {
            when (path ?: "/") {
                "/me" -> MockResponse().setResponseCode(204)
                else -> MockResponse().setResponseCode(404)
            }
        }

        ApiCallImpl(
            Endpoint(
                HttpClient.apiBaseUrl.toString(),
                "/me",
                Verb.Modify.Put(
                    "username" to "foo"
                )
            ),
            HttpClient.http,
            context
        )()

        with(lastRequest()) {
            method shouldBe "PUT"
            headers.toMultimap()["Authorization"]!![1] shouldBe "Bearer token_123"
            body.toString() shouldBe "[text=username=foo]"
        }
    }

    "should delete a resource" {
        dispatchable = {
            when (path ?: "/") {
                "/me" -> MockResponse().setResponseCode(204)
                else -> MockResponse().setResponseCode(404)
            }
        }

        ApiCallImpl(
            Endpoint(
                HttpClient.apiBaseUrl.toString(),
                "/me",
                Verb.Delete
            ),
            HttpClient.http,
            context
        )()

        with(lastRequest()) {
            method shouldBe "DELETE"
            headers.toMultimap()["Authorization"]!![1] shouldBe "Bearer token_123"
        }
    }
})
