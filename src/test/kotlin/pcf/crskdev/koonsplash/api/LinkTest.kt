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

import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestContext
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.toList
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.source
import pcf.crskdev.koonsplash.http.HttpClient
import java.io.File
import java.util.UUID

internal class LinkTest : StringSpec({

    val server = MockWebServer()

    val apiCallProvider: (String) -> ApiCall = { url -> ApiCallImpl(Endpoint(url), HttpClient.http, "", null) }

    val emptyDispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = MockResponse()
    }

    beforeTest {
        server.start()
        HttpClient.apiBaseUrl = HttpUrl.Builder()
            .scheme("http")
            .host(server.hostName)
            .port(server.port)
            .build()
            .toString()
    }

    afterTest {
        server.shutdown()
        server.dispatcher = emptyDispatcher
    }

    "should download a photo" {
        val locationHash = UUID.randomUUID().toString().replace("-", "")
        server.dispatcher = dispatcher {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk/download" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBody("""{"url":"${HttpClient.apiBaseUrl}$locationHash"}""")
                "/$locationHash" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "image/png".toMediaType())
                        .setBody(
                            Buffer().apply {
                                writeAll(
                                    javaClass
                                        .classLoader
                                        .getResource("photo_test.png")!!
                                        .openStream()
                                        .source()
                                )
                            }
                        )
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(HttpClient.apiBaseUrl + "photos/Dwu85P9SOIk/download", apiCallProvider)
        val statuses = link
            .download(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()
        statuses.find<LinkPhotoStatusCanceled>()?.apply { throw err }
        val filePath = statuses.find<LinkPhotoStatusDone>()?.resource?.url
        requireNotNull(filePath)

        filePath.endsWith("photo_test_downloaded.png") shouldBe true

        // cleanup
        File(filePath).delete()
    }
})

private fun fileFromPath(vararg segments: String): File = File(segments.joinToString(File.separator))

private fun TestContext.dispatcher(requestBlock: RecordedRequest.() -> MockResponse): Dispatcher =
    object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = request.run(requestBlock)
    }

private inline fun <reified T : ApiCall.ProgressStatus<Link.Photo>> List<ApiCall.ProgressStatus<Link.Photo>>.find(): T? =
    find { it is T } as T?

typealias LinkPhotoStatusCanceled = ApiCall.ProgressStatus.Canceled<Link.Photo>
typealias LinkPhotoStatusDone = ApiCall.ProgressStatus.Done<Link.Photo>
