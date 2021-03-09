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

package pcf.crskdev.koonsplash.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import pcf.crskdev.koonsplash.http.HttpClient

/**
 * CoroutineScope extension that creates a new [MockWebServer].
 * After coroutines are finished the server will close.
 *
 * @param body Scope.
 * @receiver MockWebServerScope
 */
fun CoroutineScope.server(body: suspend MockWebServerScope.() -> Unit) {

    val server = MockWebServer()

    class BeforeStartScopeImpl : BeforeStartScope {
        override fun enqueue(response: MockResponse) {
            server.enqueue(response)
        }
        override fun dispatcher(block: (RecordedRequest) -> MockResponse) {
            server.dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    block(request)
            }
        }
    }

    class MockWebServerScopeImpl : MockWebServerScope {

        override fun beforeStart(block: BeforeStartScope.() -> Unit) {
            BeforeStartScopeImpl().apply(block)
        }

        override fun takeRequest(): RecordedRequest = server.takeRequest()
    }

    server.run {
        start()
        HttpClient.apiBaseUrl = HttpUrl.Builder()
            .scheme("http")
            .host(server.hostName)
            .port(server.port)
            .build()
            .toUri()
        HttpClient.baseUrl = HttpClient.apiBaseUrl
        launch {
            MockWebServerScopeImpl().body()
        }.invokeOnCompletion {
            server.shutdown()
        }
    }
}

interface MockWebServerScope {

    fun beforeStart(block: BeforeStartScope.() -> Unit)

    fun takeRequest(): RecordedRequest
}

@MockWebServerMarker
interface BeforeStartScope {

    fun enqueue(response: MockResponse)

    fun dispatcher(block: (RecordedRequest) -> MockResponse)
}

@DslMarker
annotation class MockWebServerMarker
