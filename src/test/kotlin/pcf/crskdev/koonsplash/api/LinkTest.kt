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

@file:Suppress("unused")

package pcf.crskdev.koonsplash.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import pcf.crskdev.koonsplash.auth.AuthContext
import pcf.crskdev.koonsplash.auth.BrowserLauncher
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.http.HttpException
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import pcf.crskdev.koonsplash.util.StringSpecIT
import pcf.crskdev.koonsplash.util.checksum
import pcf.crskdev.koonsplash.util.fileFromPath
import pcf.crskdev.koonsplash.util.resource
import pcf.crskdev.koonsplash.util.setBodyFromFile
import pcf.crskdev.koonsplash.util.setBodyFromResource
import pcf.crskdev.koonsplash.util.toFile
import java.io.File
import java.net.URI
import java.util.UUID

/**
 * Link test for [Link] implementations.
 *
 * @constructor Create empty Link test
 */
@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@FlowPreview
internal class LinkTest : StringSpecIT({

    val apiCallProvider: (String) -> ApiCall = { url ->
        ApiCallImpl(
            Endpoint(url),
            HttpClient.http,
            KoonsplashContext.Builder().auth { AuthContext.None("") }.build()
        )
    }

    val context = KoonsplashContext.Builder().apiCaller(apiCallProvider).build()

    "should create the correct links" {
        val apiLink = Link.create(HttpClient.apiBaseUrl.resolve("/foo/bar/").toString(), mockk())
        val apiLinkNoPath = Link.create(HttpClient.apiBaseUrl.resolve("/").toString(), mockk())
        val browseLink = Link.create("http://example.xyz/", mockk())
        val downloadLink = Link.create(HttpClient.apiBaseUrl.resolve("/foo/bar/download").toString(), mockk())
        val photoLink = Link.create(HttpClient.imagesBaseUrl.resolve("/foo/bar/").toString(), mockk())

        apiLink.shouldBeInstanceOf<Link.Api>()
        apiLinkNoPath.shouldBeInstanceOf<Link.Api>()
        browseLink.shouldBeInstanceOf<Link.Browser>()
        downloadLink.shouldBeInstanceOf<Link.Download>()
        photoLink.shouldBeInstanceOf<Link.Photo>()
    }

    "should download a photo with Unsplash download policy" {
        val locationHash = UUID.randomUUID().toString().replace("-", "")
        val remoteFile = resource("photo_test.png").toFile()
        dispatchable = {
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
                        .setBodyFromFile(remoteFile)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            context = context
        )

        val statuses = link
            .downloadWithProgress(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()
        val fileUri = statuses.find<LinkPhotoStatusDone>()?.resource?.url
        requireNotNull(fileUri)

        fileUri.toString().endsWith("photo_test_downloaded.png") shouldBe true
        File(fileUri.path).checksum() shouldBe remoteFile.checksum()

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should download a photo with Imgix download policy" {
        val remoteFile = resource("photo_test.png").toFile()
        dispatchable = {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk?ixid=1" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "image/png".toMediaType())
                        .setBodyFromFile(remoteFile)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk?ixid=1"),
            Link.Download.Policy.IMGIX,
            context
        )

        val statuses = link
            .downloadWithProgress(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()
        val fileUri = statuses.find<LinkPhotoStatusDone>()?.resource?.url
        requireNotNull(fileUri)

        fileUri.toString().endsWith("photo_test_downloaded.png") shouldBe true
        File(fileUri.path).checksum() shouldBe remoteFile.checksum()

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should download a photo using extension" {
        val locationHash = UUID.randomUUID().toString().replace("-", "")
        val remoteFile = resource("photo_test.png").toFile()
        dispatchable = {
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
                        .setBodyFromResource("photo_test.png")
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val fileUri = Link
            .Download(
                HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
                context = context
            )
            .download(
                fileFromPath("src", "test", "resources"),
                "photo_test_downloaded"
            )
            .url

        fileUri.toString().endsWith("photo_test_downloaded.png") shouldBe true
        File(fileUri.path).checksum() shouldBe remoteFile.checksum()

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should default to jpg when downloading photo extension is not found" {
        val locationHash = UUID.randomUUID().toString().replace("-", "")
        val remoteFile = resource("photo_test.png").toFile()
        dispatchable = {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk/download" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader("Content-Type", "application/json".toMediaType())
                        .setBody("""{"url":"${HttpClient.apiBaseUrl}$locationHash"}""")
                "/$locationHash" ->
                    MockResponse()
                        .setResponseCode(200)
                        .setBodyFromResource("photo_test.png")
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            context = context
        )
        val statuses = link
            .downloadWithProgress(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()

        val fileUri = statuses.find<LinkPhotoStatusDone>()?.resource?.url
        requireNotNull(fileUri)

        fileUri.toString().endsWith("photo_test_downloaded.jpg") shouldBe true
        File(fileUri.path).checksum() shouldBe remoteFile.checksum()

        // cleanup
        assert(File(fileUri.path).delete())
    }

    "should have cancel status if something went wrong on fetching real url location" {
        dispatchable = {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk/download" ->
                    MockResponse()
                        .setResponseCode(500)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        val link = Link.Download(
            HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
            context = context
        )
        val statuses = link
            .downloadWithProgress(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()

        statuses.find<LinkPhotoStatusCanceled>()?.shouldNotBeNull()
    }

    "should have cancel status if something went wrong during download" {
        val locationHash = UUID.randomUUID().toString().replace("-", "")
        dispatchable = {
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
            context = context
        )
        val statuses = link
            .downloadWithProgress(fileFromPath("src", "test", "resources"), "photo_test_downloaded")
            .toList()

        statuses.find<LinkPhotoStatusCanceled>()?.shouldNotBeNull()
    }

    "should throw when downloading using extension" {
        dispatchable = {
            when (path ?: "/") {
                "/photos/Dwu85P9SOIk/download" ->
                    MockResponse()
                        .setResponseCode(500)
                else -> throw IllegalStateException("Unknown request path $path")
            }
        }

        shouldThrow<HttpException> {
            Link
                .Download(
                    HttpClient.apiBaseUrl.resolve("photos/Dwu85P9SOIk/download"),
                    context = context
                )
                .download(
                    fileFromPath("src", "test", "resources"),
                    "photo_test_downloaded"
                )
        }
    }

    "should call the ApiLink" {
        dispatchable = {
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
        val apiLink = Link.Api(HttpClient.apiBaseUrl.resolve("random"), context = context)
        val likesLink: Link.Api = apiLink.call()["likes"]()

        likesLink.shouldBeInstanceOf<Link.Api>()

        likesLink.call().isEmpty shouldBe true
    }

    "should open a browser link" {
        val launcher = mockk<BrowserLauncher>(relaxed = true)
        val link = Link.Browser(URI.create("/"), KoonsplashContext.Builder().browserLauncher(launcher).build())
        link.open()
        verify { launcher.launch(URI.create("/")) }
    }
})

private inline fun <reified T : ApiCall.Status<Link.Browser>> List<ApiCall.Status<Link.Browser>>.find(): T? =
    find { it is T } as T?

private typealias LinkPhotoStatusCanceled = ApiCall.Status.Canceled<Link.Browser>
private typealias LinkPhotoStatusDone = ApiCall.Status.Done<Link.Browser>
