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

import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.Spec
import io.kotest.core.spec.resolvedDefaultConfig
import io.kotest.core.spec.style.scopes.Lifecycle
import io.kotest.core.spec.style.scopes.RootTestRegistration
import io.kotest.core.spec.style.scopes.StringSpecScope
import io.kotest.core.test.TestCaseConfig
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import pcf.crskdev.koonsplash.http.HttpClient
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Custom [StringSpec] that uses a [MockWebServer] for remote integration tests.
 *
 * @constructor
 *
 * @param body
 */
abstract class StringSpecIT(body: StringSpecIT.() -> Unit = {}) : DslDrivenSpec(), StringSpecScope {

    /**
     * Indexing requests.
     */
    private val requestTrack = AtomicInteger(0)

    /**
     * Lock for _dispatchable_ read/write.
     */
    private val lock = ReentrantLock()

    /**
     * Convenience lambda with receiver that dispatches a [RecordedRequest] to a [MockResponse].
     *
     * This be will be used by [MockWebServer] [Dispatcher] dispatch.
     */
    @Volatile
    var dispatchable: RecordedRequest.() -> MockResponse = { MockResponse().setResponseCode(404) }
        set(value) {
            requestTrack.incrementAndGet()
            lock.withLock {
                field = value
            }
        }

    private val server = MockWebServer().apply {
        this.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = lock.withLock {
                request.run(this@StringSpecIT.dispatchable)
            }
        }
        start()
    }

    init {
        body()
    }

    init {
        HttpClient.apiBaseUrl = HttpUrl.Builder()
            .scheme("http")
            .host(server.hostName)
            .port(server.port)
            .build()
            .toUri()
    }

    final override fun afterSpec(spec: Spec) {
        try {
            server.shutdown()
        } catch (ex: IOException) {
        }
    }

    /**
     * Takes last request.
     *
     * @return RecordedRequest
     * @throws IllegalStateException if there are no recorded requests.
     */
    fun lastRequest(): RecordedRequest {
        val size = requestTrack.get()
        if (size == 0) {
            throw IllegalStateException("There are no requests")
        }
        var request: RecordedRequest? = null
        repeat(size) {
            request = server.takeRequest()
            requestTrack.decrementAndGet()
        }
        return request!!
    }

    override fun lifecycle(): Lifecycle = Lifecycle.from(this)
    override fun defaultConfig(): TestCaseConfig = resolvedDefaultConfig()
    override fun registration(): RootTestRegistration = RootTestRegistration.from(this)
}
