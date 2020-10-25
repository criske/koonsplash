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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.util.dispatcher
import pcf.crskdev.koonsplash.util.fileFromPath
import pcf.crskdev.koonsplash.util.setBodyFromFile
import java.io.File
import java.util.UUID

@ExperimentalCoroutinesApi
@FlowPreview
internal class LinkTest : StringSpec({

    val server = MockWebServer().apply { start() }

    val apiCallProvider: (String) -> ApiCall = { url -> ApiCallImpl(Endpoint(url), HttpClient.http, "", null) }

    val emptyDispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = MockResponse()
    }

    HttpClient.apiBaseUrl = HttpUrl.Builder()
        .scheme("http")
        .host(server.hostName)
        .port(server.port)
        .build()
        .toUri()

    afterTest {
        server.dispatcher = emptyDispatcher
    }

    afterSpec {
        server.shutdown()
    }

    "should create the correct links" {
        val apiLink = Link.create({ mockk() }, HttpClient.apiBaseUrl.resolve("/foo/bar/").toString())
        val browseLink = Link.create({ mockk() }, "http://example.xyz/")
        val downloadLink = Link.create({ mockk() }, HttpClient.apiBaseUrl.resolve("/foo/bar/download").toString())
        val photoLink = Link.create({ mockk() }, HttpClient.imagesBaseUrl.resolve("/foo/bar/").toString())

        apiLink.shouldBeInstanceOf<Link.Api>()
        browseLink.shouldBeInstanceOf<Link.Browser>()
        downloadLink.shouldBeInstanceOf<Link.Download>()
        photoLink.shouldBeInstanceOf<Link.Photo>()
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
                        .setBody("photo_test.png")
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            apiCallProvider
        )
        val statuses = link
            .download(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()
        val fileUri = statuses.find<LinkPhotoStatusDone>()?.resource?.url
        requireNotNull(fileUri)

        fileUri.toString().endsWith("photo_test_downloaded.png") shouldBe true

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should download a photo using extension" {
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
                        .setBodyFromFile("photo_test.png")
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val fileUri = Link
            .Download(
                HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
                apiCallProvider
            )
            .downloadToPhoto(
                fileFromPath("src", "test", "resources"),
                "photo_test_downloaded"
            )
            .url

        fileUri.toString().endsWith("photo_test_downloaded.png") shouldBe true

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should default to jpg when downloading photo extension is not found" {
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
                        .setBodyFromFile("photo_test.png")
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            apiCallProvider
        )
        val statuses = link
            .download(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()

        val fileUri = statuses.find<LinkPhotoStatusDone>()?.resource?.url
        requireNotNull(fileUri)

        fileUri.toString().endsWith("photo_test_downloaded.jpg") shouldBe true

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should have cancel status if something went wrong on fetching real url location" {
        server.dispatcher = dispatcher {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk/download" ->
                    MockResponse()
                        .setResponseCode(500)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            apiCallProvider
        )
        val statuses = link
            .download(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()

        statuses.find<LinkPhotoStatusCanceled>()?.shouldNotBeNull()
    }

    "should have cancel status if something went wrong during download" {
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
                        .setResponseCode(500)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            apiCallProvider
        )
        val statuses = link
            .download(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()

        statuses.find<LinkPhotoStatusCanceled>()?.shouldNotBeNull()
    }

    "should throw when downloading using extension" {
        server.dispatcher = dispatcher {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk/download" ->
                    MockResponse()
                        .setResponseCode(500)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        shouldThrow<IllegalStateException> {
            Link
                .Download(
                    HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
                    apiCallProvider
                )
                .downloadToPhoto(
                    fileFromPath("src", "test", "resources"),
                    "photo_test_downloaded"
                )
        }
    }

    "should call the ApiLink" {
        server.dispatcher = dispatcher {
            when (path ?: "/") {
                "/random" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBody(""" {"likes": "${HttpClient.apiBaseUrl.resolve("likes")}"} """)
                "/likes" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBody("[]")
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }
        val apiLink = Link.Api(HttpClient.apiBaseUrl.resolve("random"), apiCallProvider)
        val likesLink: Link.Api = apiLink.call()["likes"]()

        likesLink.shouldBeInstanceOf<Link.Api>()

        likesLink.call().isEmpty shouldBe true
    }
})

private inline fun <reified T : ApiCall.ProgressStatus<Link.Photo>> List<ApiCall.ProgressStatus<Link.Photo>>.find(): T? = find { it is T } as T?

private typealias LinkPhotoStatusCanceled = ApiCall.ProgressStatus.Canceled<Link.Photo>
private typealias LinkPhotoStatusDone = ApiCall.ProgressStatus.Done<Link.Photo>
